package be.nabu.libs.swagger.parser;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import be.nabu.libs.types.base.CollectionFormat;
import be.nabu.libs.converter.ConverterFactory;
import be.nabu.libs.property.ValueUtils;
import be.nabu.libs.property.api.Value;
import be.nabu.libs.resources.URIUtils;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerParameter.ParameterLocation;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.OAuth2Flow;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition.SecurityType;
import be.nabu.libs.swagger.api.SwaggerSecuritySetting;
import be.nabu.libs.types.SimpleTypeWrapperFactory;
import be.nabu.libs.types.TypeRegistryImpl;
import be.nabu.libs.types.TypeUtils;
import be.nabu.libs.types.api.ComplexContent;
import be.nabu.libs.types.api.ComplexType;
import be.nabu.libs.types.api.DefinedType;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.ModifiableType;
import be.nabu.libs.types.api.ModifiableTypeRegistry;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Type;
import be.nabu.libs.types.base.ComplexElementImpl;
import be.nabu.libs.types.base.SimpleElementImpl;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.java.BeanType;
import be.nabu.libs.types.map.MapContent;
import be.nabu.libs.types.map.MapTypeGenerator;
import be.nabu.libs.types.properties.CollectionFormatProperty;
import be.nabu.libs.types.properties.CommentProperty;
import be.nabu.libs.types.properties.EnumerationProperty;
import be.nabu.libs.types.properties.FormatProperty;
import be.nabu.libs.types.properties.MaxExclusiveProperty;
import be.nabu.libs.types.properties.MaxInclusiveProperty;
import be.nabu.libs.types.properties.MaxLengthProperty;
import be.nabu.libs.types.properties.MaxOccursProperty;
import be.nabu.libs.types.properties.MinExclusiveProperty;
import be.nabu.libs.types.properties.MinInclusiveProperty;
import be.nabu.libs.types.properties.MinLengthProperty;
import be.nabu.libs.types.properties.MinOccursProperty;
import be.nabu.libs.types.properties.PatternProperty;
import be.nabu.libs.types.properties.TimezoneProperty;
import be.nabu.libs.types.structure.DefinedStructure;

/** in theory there is a descriminator field where you could have (as far as I can tell):
 Animal:
    type: object
    discriminator: petType
    properties:
      commonName:
        type: string
      petType:
      	type: string
      	
 Cat extends animal
 Dog extends animal
 
 Method return animal
 
 At runtime it can be either a cat or a dog and the field "petType" in this case must contain either Cat or Dog (exact naming)
 The petType must also be in the list of properties of the type _and_ be in the required list.
 */
public class SwaggerParser {

	private TimeZone timezone;
	private boolean allowRemoteResolving = false;
	private List<SwaggerSecuritySetting> globalSecurity;
	
	public static void main(String...args) throws IOException {
		URL url = new URL("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v2.0/json/petstore.json");
		url = new URL("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v2.0/json/petstore-expanded.json");
		InputStream openStream = url.openStream();
		try {
			SwaggerDefinition definition = new SwaggerParser().parse("my.swagger", openStream);
			System.out.println(definition);
		}
		finally {
			openStream.close();
		}
	}
	
	public MapContent parseJson(String id, InputStream input) {
		try {
			JSONBinding binding = new JSONBinding(new MapTypeGenerator(true), Charset.forName("UTF-8"));
			binding.setAllowDynamicElements(true);
			binding.setAddDynamicElementDefinitions(true);
			binding.setAllowRaw(true);
			binding.setParseNumbers(true);
			binding.setSetEmptyArrays(true);
			return (MapContent) binding.unmarshal(input, new Window[0]);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public SwaggerDefinition parse(String id, InputStream input) {
		try {
			MapContent content = parseJson(id, input);
			if (allowRemoteResolving) {
				resolveRemoteRefs(content);
			}
			SwaggerDefinitionImpl definition = new SwaggerDefinitionImpl(id);
			if (content.get("info") != null) {
				definition.setInfo(SwaggerInfoImpl.parse((ComplexContent) content.get("info")));
			}
			parseInitial(definition, content);
			parseDefinitions(definition, content);
			parseSecurityDefinitions(definition, content);
			definition.setGlobalSecurity(parseSecurity(content));
			
			definition.setParameters(parseParameters(definition, (MapContent) content.get("parameters")));
			
			if (content.get("paths") != null) {
				definition.setPaths(parsePaths(definition, (MapContent) content.get("paths")));
			}
			
			return definition;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public MapContent resolveRemoteRefs(MapContent content) {
		return resolveRemoteRefs(content, new HashMap<String, MapContent>());
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private MapContent resolveRemoteRefs(MapContent content, Map<String, MapContent> parsed) {
		Map map = content.getContent();
		for (Object key : map.keySet()) {
			Object value = map.get(key);
			if (key.toString().equals("$ref")) {
				if (value instanceof String && ((String) value).matches("^[\\w]+:/.*")) {
					if (!parsed.containsKey(value)) {
						try {
							System.out.println("Retrieving: " + value);
							URL url = new URL(URIUtils.encodeURI((String) value));
							InputStream stream = new BufferedInputStream(url.openStream());
							try {
								JSONBinding binding = new JSONBinding(new MapTypeGenerator(true), Charset.forName("UTF-8"));
								binding.setAllowDynamicElements(true);
								binding.setAddDynamicElementDefinitions(true);
								binding.setAllowRaw(true);
								binding.setParseNumbers(true);
								binding.setSetEmptyArrays(true);
								MapContent childContent = (MapContent) binding.unmarshal(stream, new Window[0]);
								parsed.put((String) value, childContent);
								// recursively resolve it
								resolveRemoteRefs(childContent, parsed);
							}
							finally {
								stream.close();
							}
						}
						catch (Exception e) {
							throw new RuntimeException(e);
						}
					}
					return parsed.get(value);
				}
			}
			else if (value instanceof MapContent) {
				map.put(key, resolveRemoteRefs((MapContent) value, parsed));
			}
			else if (value instanceof List) {
				for (int i = 0; i < ((List) value).size(); i++) {
					if (((List) value).get(i) instanceof MapContent) {
						((List) value).set(i, resolveRemoteRefs((MapContent) ((List) value).get(i), parsed));
					}
				}
			}
		}
		return content;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, SwaggerParameter> parseParameters(SwaggerDefinition definition, MapContent content) throws ParseException {
		Map<String, SwaggerParameter> parameters = new HashMap<String, SwaggerParameter>();
		if (content != null) {
			for (String key : ((Map<String, Object>) content.getContent()).keySet()) {
				Object object = ((Map<String, Object>) content.getContent()).get(key);
				parameters.put(key, parseParameter(definition, ((MapContent) object).getContent()));
			}
		}
		return parameters;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, SwaggerParameter> parseParameters(SwaggerDefinition definition, List<MapContent> content) throws ParseException {
		Map<String, SwaggerParameter> parameters = new HashMap<String, SwaggerParameter>();
		if (content != null) {
			for (MapContent single : content) {
				SwaggerParameter parseParameter = parseParameter(definition, single.getContent());
				parameters.put(parseParameter.getName(), parseParameter);
			}
		}
		return parameters;
	}
	
	@SuppressWarnings({ "unchecked", "incomplete-switch" })
	private static void parseSecurityDefinitions(SwaggerDefinitionImpl definition, MapContent content) {
		MapContent securityDefinitions = (MapContent) content.getContent().get("securityDefinitions");
		if (securityDefinitions != null) {
			List<SwaggerSecurityDefinition> securities = new ArrayList<SwaggerSecurityDefinition>();
			for (Object name : securityDefinitions.getContent().keySet()) {
				SwaggerSecurityDefinitionImpl security = new SwaggerSecurityDefinitionImpl();
				MapContent securityContent = (MapContent) securityDefinitions.getContent().get(name);
				security.setType(SecurityType.valueOf((String) securityContent.getContent().get("type")));
				security.setName((String) name);
				security.setDescription((String) securityContent.getContent().get("description"));
				switch(security.getType()) {
					case apiKey:
						if (securityContent.getContent().containsKey("in")) {
							security.setLocation(ParameterLocation.valueOf(((String) securityContent.getContent().get("in")).toUpperCase()));
						}
						security.setFieldName((String) securityContent.getContent().get("name"));
					break;
					case oauth2:
						if (securityContent.getContent().containsKey("flow")) {
							security.setFlow(OAuth2Flow.valueOf((String) securityContent.getContent().get("flow")));
						}
						security.setTokenUrl((String) securityContent.getContent().get("tokenUrl"));
						security.setAuthorizationUrl((String) securityContent.getContent().get("authorizationUrl"));
						security.setScopes((Map<String, String>) ((MapContent) securityContent.getContent().get("scopes")).getContent());
					break;
				}
				securities.add(security);
			}
			definition.setSecurityDefinitions(securities);
		}
	}
	
	private List<SwaggerPath> parsePaths(SwaggerDefinitionImpl definition, MapContent content) throws ParseException {
		List<SwaggerPath> paths = new ArrayList<SwaggerPath>();
		for (Object path : content.getContent().keySet()) {
			SwaggerPathImpl swaggerPath = new SwaggerPathImpl();
			swaggerPath.setPath((String) path);
			MapContent methodMap = (MapContent) content.get((String) path);
			if (methodMap != null) {
				swaggerPath.setMethods(parseMethods(swaggerPath, definition, methodMap));
			}
			paths.add(swaggerPath);
		}
		return paths;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private List<SwaggerMethod> parseMethods(SwaggerPathImpl path, SwaggerDefinitionImpl definition, MapContent content) throws ParseException {
		List<SwaggerMethod> methods = new ArrayList<SwaggerMethod>();
		
		Map<String, SwaggerParameter> inheritedParameters = parseParameters(definition, (List<MapContent>) content.getContent().get("parameters"));
		for (Object method : content.getContent().keySet()) {
			if ("parameters".equalsIgnoreCase(method.toString())) {
				continue;
			}
			MapContent methodContent = (MapContent) content.getContent().get(method);
			SwaggerMethodImpl swaggerMethod = new SwaggerMethodImpl();
			swaggerMethod.setMethod((String) method);
			swaggerMethod.setConsumes((List<String>) methodContent.get("consumes"));
			swaggerMethod.setProduces((List<String>) methodContent.get("produces"));
			swaggerMethod.setSchemes((List<String>) methodContent.get("schemes"));
			swaggerMethod.setDescription((String) methodContent.get("description"));
			swaggerMethod.setSummary((String) methodContent.get("summary"));
			swaggerMethod.setOperationId((String) methodContent.get("operationId"));
			swaggerMethod.setTags((List<String>) methodContent.get("tags"));
			swaggerMethod.setDeprecated((Boolean) methodContent.get("deprecated"));
			swaggerMethod.setExtensions(parseExtensions(methodContent));
			
			// make sure we always have an operation id, even if it is a fictive one
			// we need it to make sure that we can describe the methods (e.g. for whitelisting)
			cleanupOperationId(path, swaggerMethod);
			
			if (methodContent.get("documentation") != null) {
				swaggerMethod.setDocumentation(SwaggerDocumentationImpl.parse((ComplexContent) methodContent.get("documentation")));
			}
			if (methodContent.get("parameters") != null) {
				List<Object> parameters = (List<Object>) methodContent.get("parameters");
				List<SwaggerParameter> list = new ArrayList<SwaggerParameter>();
				List<String> overriddenParameters = new ArrayList<String>();
				for (Object object : parameters) {
					MapContent map = (MapContent) object;
					SwaggerParameter parameter = parseParameter(definition, map.getContent());
					list.add(parameter);
					overriddenParameters.add(parameter.getName());
				}
				for (String key : inheritedParameters.keySet()) {
					if (!overriddenParameters.contains(key)) {
						list.add(inheritedParameters.get(key));
					}
				}
				swaggerMethod.setParameters(list);
			}
			if (methodContent.get("responses") != null) {
				List<SwaggerResponse> responses = new ArrayList<SwaggerResponse>();
				MapContent responsesContent = (MapContent) methodContent.get("responses");
				for (Object code : responsesContent.getContent().keySet()) {
					MapContent responseContent = (MapContent) responsesContent.getContent().get(code);
					SwaggerResponseImpl response = new SwaggerResponseImpl();
					if (!code.equals("default")) {
						response.setCode(Integer.parseInt(code.toString()));
					}
					response.setDescription((String) responseContent.get("description"));
					MapContent schemaContent = (MapContent) responseContent.get("schema");
					if (schemaContent != null) {
						Type type;
						// it is usually a reference
						if (schemaContent.get("$ref") != null) {
							type = findType(definition, (String) schemaContent.get("$ref"), null);
						}
						// but it "can" theoretically be a simple type as well, note that this may not work well...
						else {
							type = parseDefinedType(definition, "body", schemaContent.getContent(), false, false, new HashMap<String, Type>());
//							throw new ParseException("Only supporting array or $ref for responses, found: " + schemaContent.getContent(), 0);
						}
						if (type instanceof ComplexType) {
							response.setElement(new ComplexElementImpl("body", (ComplexType) type, null));
						}
						else {
							response.setElement(new SimpleElementImpl("body", (SimpleType<?>) type, null));
						}
					}
					MapContent headersContent = (MapContent) responseContent.get("headers");
					if (headersContent != null) {
						List<SwaggerParameter> headers = new ArrayList<SwaggerParameter>();
						for (Object name : headersContent.getContent().keySet()) {
							// add the name to the content to better match the property spec
							((MapContent) headersContent.getContent().get(name)).getContent().put("name", name);
							headers.add(parseParameter(definition, ((MapContent) headersContent.getContent().get(name)).getContent()));
						}
						response.setHeaders(headers);
					}
					responses.add(response);
				}
				swaggerMethod.setResponses(responses);
			}
			
			// based on example: https://github.com/OAI/OpenAPI-Specification/blob/master/fixtures/v2.0/json/resources/securityExample.json
			if (methodContent.get("security") != null) {
				List<SwaggerSecuritySetting> settings = parseSecurity(methodContent);
				if (!settings.isEmpty()) {
					swaggerMethod.setSecurity(settings);
				}
			}
			// inherit global security
			else {
				swaggerMethod.setSecurity(definition.getGlobalSecurity());
			}
			methods.add(swaggerMethod);
		}
		return methods;
	}
	
	public List<SwaggerSecuritySetting> getGlobalSecurity() {
		return globalSecurity;
	}

	public void setGlobalSecurity(List<SwaggerSecuritySetting> globalSecurity) {
		this.globalSecurity = globalSecurity;
	}

	@SuppressWarnings("unchecked")
	private List<SwaggerSecuritySetting> parseSecurity(MapContent mapContent) {
		List<Object> securities = (List<Object>) mapContent.get("security");
		List<SwaggerSecuritySetting> settings = new ArrayList<SwaggerSecuritySetting>();
		if (securities != null) {
			for (Object security : securities) {
				// not sure if there can ever be more than one security entry per object, this is an odd part of the spec
				MapContent securityContent = (MapContent) security;
				for (Object name : securityContent.getContent().keySet()) {
					SwaggerSecuritySettingImpl impl = new SwaggerSecuritySettingImpl();
					impl.setName((String) name);
					impl.setScopes((List<String>) securityContent.getContent().get(name));
					settings.add(impl);
				}
			}
		}
		return settings;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseExtensions(MapContent methodContent) {
		Map<String, Object> extensions = new HashMap<String, Object>();
		for (String key : ((Map<String, ?>) methodContent.getContent()).keySet()) {
			if (key.startsWith("x-")) {
				extensions.put(key.substring("x-".length()), methodContent.get(key));
			}
		}
		return !extensions.isEmpty() ? extensions : null;
	}

	@SuppressWarnings("unchecked")
	private void parseInitial(SwaggerDefinitionImpl definition, MapContent content) {
		if ("2.0".equals(content.get("swagger"))) {
			definition.setDefinitionType("swagger");
			definition.setVersion((String) content.get("swagger"));	
		}
		else if ("3.0.0".equals(content.get("openapi"))) {
			definition.setDefinitionType("openapi");
			definition.setVersion((String) content.get("openapi"));
		}
		else {
			throw new IllegalArgumentException("Currently only swagger 2.0 and openapi 3.0.0 are supported");
		}
		definition.setHost((String) content.get("host"));
		definition.setBasePath((String) content.get("basePath"));
		definition.setSchemes((List<String>) content.get("schemes"));
		definition.setConsumes((List<String>) content.get("consumes"));
		definition.setProduces((List<String>) content.get("produces"));
	}
	
	private void parseComponents(SwaggerDefinitionImpl definition, MapContent content) throws ParseException {
		definition.setRegistry(new TypeRegistryImpl());
		MapContent components = (MapContent) content.get("components");
		if (components != null) {
			
		}
	}
	
	@SuppressWarnings("unchecked")
	private void parseDefinitions(SwaggerDefinitionImpl definition, MapContent content) throws ParseException {
		definition.setRegistry(new TypeRegistryImpl());
		MapContent definitions = (MapContent) content.get("definitions");
		if (definitions != null) {
			List<String> previousFailed = null;
			List<String> failed = null;
			while (previousFailed == null || failed.size() < previousFailed.size()) {
				Collection<?> toParse = failed == null ? definitions.getContent().keySet() : failed;
				previousFailed = failed;
				failed = new ArrayList<String>();
				for (Object typeName : toParse) {
					try {
						parseDefinedType(definition, (String) typeName, ((MapContent) definitions.getContent().get(typeName)).getContent(), true, false, new HashMap<String, Type>());
					}
					catch (ParseException e) {
						// we should repeat
						if (e.getErrorOffset() == 1) {
							failed.add((String) typeName);
						}
						else {
							throw e;
						}
					}
				}
			}
			if (failed != null && !failed.isEmpty()) {
				throw new ParseException("Could not parse all the elements: " + failed, 0);
			}
		}
	}
	
	public static boolean isValid(char character, boolean first, boolean last, boolean allowDots) {
		if (!allowDots && character == '.') {
			return false;
		}
		// dots are also allowed for namespacing
		return (!first && character >= 48 && character <= 57) || (!first && !last && character == 46) || (character >= 65 && character <= 90) || (character >= 97 && character <= 122);
	}
	
	private static void cleanupOperationId(SwaggerPath path, SwaggerMethodImpl method) {
		method.setOperationId(SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId()));
	}
	
	public static String cleanup(String name) {
		return cleanup(name, true);
	}
	
	public static String cleanup(String name, boolean allowDots) {
		StringBuilder builder = new StringBuilder();
		boolean uppercase = false;
		for (int i = 0; i < name.length(); i++) {
			if (builder.toString().isEmpty()) {
				if (isValid(name.charAt(i), true, i == name.length() - 1, allowDots)) {
					builder.append(name.substring(i, i + 1).toLowerCase());
				}
			}
			else if (i == name.length() - 1) {
				if (isValid(name.charAt(i), false, true, allowDots)) {
					builder.append(name.substring(i, i + 1));
				}
			}
			else if (isValid(name.charAt(i), false, false, allowDots)) {
				if (uppercase) {
					builder.append(name.substring(i, i + 1).toUpperCase());
					uppercase = false;
				}
				else {
					builder.append(name.substring(i, i + 1));
				}
			}
			else {
				uppercase = true;
			}
		}
		return builder.toString();
	}
	
	private Type findType(SwaggerDefinition definition, String name, Map<String, Type> ongoing) throws ParseException {
		if (name.startsWith("#/definitions/")) {
			name = name.substring("#/definitions/".length());
		}
		Type type = definition.getRegistry().getComplexType(definition.getId(), name);
		if (type == null) {
			type = definition.getRegistry().getSimpleType(definition.getId(), name);
		}
		if (type == null && ongoing.containsKey(name)) {
			return ongoing.get(name);
		}
		if (type == null) {
			name = cleanup(name);
			type = definition.getRegistry().getComplexType(definition.getId(), name);
			if (type == null) {
				type = definition.getRegistry().getSimpleType(definition.getId(), name);
			}
			if (type == null && ongoing.containsKey(name)) {
				return ongoing.get(name);
			}
		}
		if (type == null) {
			throw new ParseException("Can not resolve type: " + name, 1);
		}
		return type;
	}
	
	private SwaggerParameter parseParameter(SwaggerDefinition definition, Map<String, Object> content) throws ParseException {
		if (content.containsKey("$ref")) {
			SwaggerParameter swaggerParameter = ((SwaggerDefinitionImpl) definition).getParameters().get(content.get("$ref").toString().substring("#/parameters/".length()));
			if (swaggerParameter == null) {
				throw new ParseException("Could not find referenced parameter: " + content.get("$ref"), 0);
			}
			return swaggerParameter;
		}
		SwaggerParameterImpl parameter = new SwaggerParameterImpl();
		parameter.setName((String) content.get("name"));
		// we currently do nothing with these parameters
		parameter.setDefaultValue(content.get("default"));
		parameter.setMultipleOf((Number) content.get("multipleOf"));
		parameter.setUnique((Boolean) content.get("uniqueItems"));
		parameter.setAllowEmptyValue((Boolean) content.get("allowEmptyValue"));
		String collectionFormat = (String) content.get("collectionFormat");
		if (collectionFormat != null) {
			parameter.setCollectionFormat(CollectionFormat.valueOf(collectionFormat.toUpperCase()));
		}
		parameter.setElement(parseParameterElement(definition, content));
		if (parameter.getCollectionFormat() != null) {
			parameter.getElement().setProperty(new ValueImpl<CollectionFormat>(CollectionFormatProperty.getInstance(), parameter.getCollectionFormat()));
		}
		String in = (String) content.get("in");
		parameter.setLocation(in == null ? null : ParameterLocation.valueOf(in.toUpperCase()));
		return parameter;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Element<?> parseParameterElement(SwaggerDefinition definition, Map<String, Object> content) throws ParseException {
		String name = cleanup((String) content.get("name"));
		Type type;
		if (content.get("schema") != null) {
			MapContent schema = (MapContent) content.get("schema");
			if (schema.get("$ref") != null) {
				type = findType(definition, (String) schema.get("$ref"), null);
			}
			// if it has no type but it does have properties, it is an object
			else if (schema.get("type") != null || schema.get("properties") != null) {
				type = parseDefinedType(definition, name, schema.getContent(), false, true, new HashMap<String, Type>());
			}
			else {
				throw new ParseException("Unsupported use of schema for element '" + name + "': " + schema, 0);
			}
		}
		else {
			type = parseDefinedType(definition, name, content, false, true, new HashMap<String, Type>());
		}
		Boolean required = (Boolean) content.get("required");
		if (type instanceof SimpleType) {
			return new SimpleElementImpl(name, (SimpleType<?>) type, null, new ValueImpl(MinOccursProperty.getInstance(), required == null || !required ? 0 : 1));
		}
		else {
			return new ComplexElementImpl(name, (ComplexType) type, null, new ValueImpl(MinOccursProperty.getInstance(), required == null || !required ? 0 : 1));
		}
	}
	// we can't expose inline simple types as defined because you might have a lot with the same name and different (or even the same) values, the name is only the local element
	// the ongoing allows for circular references to oneself
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Type parseDefinedType(SwaggerDefinition definition, String name, Map<String, Object> content, boolean isRoot, boolean checkUndefinedRequired, Map<String, Type> ongoing) throws ParseException {
		String type = (String) content.get("type");
		
		String cleanedUpName = cleanup(name);
		String typeId = definition.getId() + ".types." + cleanedUpName;
		List<Value<?>> values = new ArrayList<Value<?>>();
		
		ModifiableType result;
		// complex type
		if (type == null || type.equals("object")) {
			DefinedStructure structure = new DefinedStructure();
			if (isRoot) {
				structure.setNamespace(definition.getId());
			}
			structure.setName(name);
			structure.setId(typeId);
			
			ongoing.put(name, structure);
			
			// allows for composition, it seems that multiple inheritance is possible which we do not support
			if (content.get("allOf") != null) {
				List<Object> allOf = (List<Object>) content.get("allOf");
				// first find the root we are extending (if any)
				for (Object single : allOf) {
					Map<String, Object> singleMap = ((MapContent) single).getContent();
					if (singleMap.containsKey("$ref")) {
						// the first ref will be mapped as a supertype
						if (structure.getSuperType() == null) {
							Type superType = findType(definition, (String) singleMap.get("$ref"), ongoing);
							if (superType == null) {
								throw new ParseException("Can not find super type: " + singleMap.get("$ref"), 1);
							}
							structure.setSuperType(superType);
						}
						// other refs are expanded in it
						else {
							Type superType = findType(definition, (String) singleMap.get("$ref"), ongoing);
							if (superType == null) {
								throw new ParseException("Can not find super type: " + singleMap.get("$ref"), 1);
							}
							if (superType instanceof ComplexType) {
								for (Element<?> child : TypeUtils.getAllChildren((ComplexType) superType)) {
									if (child.getType() instanceof ComplexType) {
										structure.add(new ComplexElementImpl(child.getName(), (ComplexType) child.getType(), structure, child.getProperties()));
									}
									else {
										structure.add(new SimpleElementImpl(child.getName(), (SimpleType<?>) child.getType(), structure, child.getProperties()));
									}
								}
							}
							else {
								throw new ParseException("Can only unfold a complex type when doing multiple extensions", 2);
							}
						}
					}
				}
				// find all none-reference extensions
				for (Object single : allOf) {
					Map<String, Object> singleMap = ((MapContent) single).getContent();
					if (!singleMap.containsKey("$ref")) {
						parseStructureProperties(definition, name, ((MapContent) single).getContent(), structure, (MapContent) ((MapContent) single).get("properties"), ongoing);
					}
				}
			}
			else {
				MapContent properties = (MapContent) content.get("properties");
				if (properties != null) {
					parseStructureProperties(definition, name, content, structure, properties, ongoing);
				}
			}
			
			// if the structure has no fields, make it a generic object instead (unless it is a root, because then it is referred to by other types and must be resolvable)
			if (!isRoot && TypeUtils.getAllChildren(structure).isEmpty()) {
				result = new BeanType<Object>(Object.class);
			}
			else {
				result = structure;
			}
		}
		else if (type.equals("array")) {
			// the actual type is in "items"
			MapContent items = (MapContent) content.get("items");
			
			Type parsedDefinedType;
			
			// if no items are specified, we assume a string array
			if (items == null) {
			//	throw new ParseException("Found an array instance without an items definition for: " + name, 0);
				parsedDefinedType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
			}
			else {
				parsedDefinedType = items.get("$ref") == null 
					? parseDefinedType(definition, name, items.getContent(), false, checkUndefinedRequired, ongoing)
					: findType(definition, (String) items.get("$ref"), ongoing);
			}
			
			// we need to extend it to add the fucked up max/min occurs properties...
			// this extension does not need to be registered globally (in general)
			// nabu allows for casting in parents to children, so at runtime you can create a parent instance and cast it to the child
			// so this will work transparently...
			if (parsedDefinedType instanceof Marshallable) {
				result = new MarshallableSimpleTypeExtension(
					!isRoot && parsedDefinedType instanceof DefinedType ? ((DefinedType) parsedDefinedType).getId() : typeId, 
					isRoot ? definition.getId() : null, 
					name, 
					(SimpleType) parsedDefinedType
				);
			}
			else if (parsedDefinedType instanceof SimpleType) {
				result = new SimpleTypeExtension(
					!isRoot && parsedDefinedType instanceof DefinedType ? ((DefinedType) parsedDefinedType).getId() : typeId, 
					isRoot ? definition.getId() : null, 
					name, 
					(SimpleType) parsedDefinedType
				);
			}
			else {
				DefinedStructure structure = new DefinedStructure();
				structure.setSuperType(parsedDefinedType);
				if (isRoot) {
					structure.setNamespace(definition.getId());
					structure.setName(name);
					structure.setId(typeId);
				}
				else {
					structure.setName(name);
					structure.setId(parsedDefinedType instanceof DefinedType ? ((DefinedType) parsedDefinedType).getId() : typeId);
				}
//				if (isRoot && parsedDefinedType instanceof DefinedType && ((DefinedType) parsedDefinedType).getId().equals(structure.getId())) {
//					throw new ParseException("No unique naming: " + structure.getId(), 0);
//				}
				result = structure;
			}
			
			Number maxOccurs = (Number) content.get("maxItems");
			Number minOccurs = (Number) content.get("minItems");
			
			int defaultMaxOccurs = result instanceof SimpleType && ((SimpleType<?>) result).getInstanceClass().equals(byte[].class) ? 1 : 0;
			values.add(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), maxOccurs == null ? defaultMaxOccurs : maxOccurs.intValue()));
			values.add(new ValueImpl<Integer>(MinOccursProperty.getInstance(), minOccurs == null ? 0 : minOccurs.intValue()));
		}
		// simple type
		else {
			SimpleType<?> simpleType;
			// note that the "format" is only indicative, there are a few listed formats you should use
			// but apart from that you can use any format you choose
			String format = (String) content.get("format");
			if (type.equals("number")) {
				if (format == null || format.equals("double")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Double.class);
				}
				else if (format.equals("float")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Float.class);
				}
				else {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Double.class);
					values.add(new ValueImpl<String>(CommentProperty.getInstance(), "Unsupported Format: " + format));
				}
			}
			else if (type.equals("integer")) {
				if (format == null || format.equals("int32")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Integer.class);
				}
				else if (format.equals("int64")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Long.class);
				}
				else if (format.equalsIgnoreCase("bigInteger")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(BigInteger.class);
				}
				else if (format.equalsIgnoreCase("bigDecimal")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(BigDecimal.class);
				}
				else {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Long.class);
					values.add(new ValueImpl<String>(CommentProperty.getInstance(), "Unsupported Format: " + format));
				}
			}
			else if (type.equals("boolean")) {
				simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Boolean.class);
			}
			else if (type.equals("file")) {
				simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(InputStream.class);
			}
			// we put all unrecognized types into a string as well to be lenient (e.g. the github swagger had a type "null", presumably an error in generation...)
			else { // if (type.equals("string"))
				if (!type.equals("string") || format == null || format.equals("string") || format.equals("password")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
				}
				// unofficial, added for digipolis
				else if (format.equals("uri")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(URI.class);
				}
				else if (format.equals("byte")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(byte[].class);
				}
				else if (format.equals("binary")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(InputStream.class);
				}
				else if (format.equals("date")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Date.class);
					values.add(new ValueImpl<String>(FormatProperty.getInstance(), "date"));
					if (timezone != null) {
						values.add(new ValueImpl<TimeZone>(TimezoneProperty.getInstance(), timezone));
					}
				}
				// don't set any additional properties for dateTime, this is the default and we avoid generating some unnecessary simple types
				else if (format.equals("date-time")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Date.class);
					if (timezone != null) {
						values.add(new ValueImpl<TimeZone>(TimezoneProperty.getInstance(), timezone));
					}
				}
				else if (format.equals("uuid")) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(UUID.class);
				}
				else {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
					values.add(new ValueImpl<String>(CommentProperty.getInstance(), "Unsupported Format: " + format));
				}
			}
			
			Boolean required = (Boolean) content.get("required");
			// the default value for required (false) is the opposite of the minoccurs
			// if it is not specified, do we want it to be inserted?
			if ((required == null && checkUndefinedRequired) || (required != null && !required)) {
				values.add(new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0));
			}
			
			result = new MarshallableSimpleTypeExtension(typeId, isRoot ? definition.getId() : null, name, simpleType);
		}
		
		if (isRoot) {
			if (result instanceof SimpleType) {
				((ModifiableTypeRegistry) definition.getRegistry()).register((SimpleType<?>) result);
			}
			else {
				((ModifiableTypeRegistry) definition.getRegistry()).register((ComplexType) result);
			}
		}
		
		// common shit
		String description = (String) content.get("description");
		if (description != null) {
			values.add(new ValueImpl<String>(CommentProperty.getInstance(), description));
		}
		Number maximum = (Number) content.get("maximum");
		Boolean exclusiveMaximum = (Boolean) content.get("exclusiveMaximum");
		if (maximum != null) {
			Object convert = ConverterFactory.getInstance().getConverter().convert(maximum, ((SimpleType<?>) result).getInstanceClass());
			values.add(new ValueImpl(exclusiveMaximum == null || !exclusiveMaximum ? new MaxInclusiveProperty() : new MaxExclusiveProperty(), convert));
		}
		
		Number minimum = (Number) content.get("minimum");
		Boolean exclusiveMinimum = (Boolean) content.get("exclusiveMinimum");
		if (minimum != null) {
			Object convert = ConverterFactory.getInstance().getConverter().convert(minimum, ((SimpleType<?>) result).getInstanceClass());
			values.add(new ValueImpl(exclusiveMinimum == null || !exclusiveMinimum ? new MinInclusiveProperty() : new MinExclusiveProperty(), convert));
		}
		
		Number maxLength = (Number) content.get("maxLength");
		if (maxLength != null) {
			values.add(new ValueImpl(MaxLengthProperty.getInstance(), maxLength.intValue()));
		}
		
		Number minLength = (Number) content.get("minLength");
		if (minLength != null) {
			values.add(new ValueImpl(MinLengthProperty.getInstance(), minLength.intValue()));
		}
		
		List<?> enumValues = (List<?>) content.get("enum");
		if (enumValues != null && !enumValues.isEmpty()) {
			values.add(new ValueImpl(new EnumerationProperty(), enumValues));
		}
		
		String pattern = (String) content.get("pattern");
		if (pattern != null) {
			values.add(new ValueImpl(PatternProperty.getInstance(), pattern));
		}
		
		// if we have a simple type with no additional settings and it is not a root definition, unwrap it to the original simple type
		if (values.isEmpty() && !isRoot && result instanceof SimpleType) {
			return result.getSuperType();
		}
		else {
			result.setProperty(values.toArray(new Value[values.size()]));
			return result;
		}
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void parseStructureProperties(SwaggerDefinition definition, String name, Map<String, Object> content, DefinedStructure structure, MapContent properties, Map<String, Type> ongoing) throws ParseException {
		List<String> required = (List<String>) content.get("required");
		for (Object key : properties.getContent().keySet()) {
			MapContent propertyContent = (MapContent) properties.getContent().get(key);
			String reference = (String) propertyContent.get("$ref");
			Type childType;
			if (reference != null) {
				childType = findType(definition, reference, ongoing);
			}
			else {
				childType = parseDefinedType(definition, (String) key, propertyContent.getContent(), false, false, ongoing);
			}
			if (childType instanceof SimpleType) {
				structure.add(new SimpleElementImpl((String) key, (SimpleType<?>) childType, structure, new ValueImpl<Integer>(MinOccursProperty.getInstance(), required == null || !required.contains(key) ? 0 : 1)));
			}
			// if we have a complex type that extends "Object" and has no other properties, unwrap it
			// ideally the parseDefinedType should probably be updated to parseElement or something so we don't need to extend types to transfer information...
			else if (childType instanceof ComplexType && TypeUtils.getAllChildren((ComplexType) childType).isEmpty() && ((ComplexType) childType).getSuperType() instanceof BeanType && ((BeanType<?>) ((ComplexType) childType).getSuperType()).getBeanClass().equals(Object.class)) {
				ComplexElementImpl element = new ComplexElementImpl((String) key, (ComplexType) childType.getSuperType(), structure, new ValueImpl<Integer>(MinOccursProperty.getInstance(), required == null || !required.contains(key) ? 0 : 1));
				// inherit properties like maxOccurs
				Integer maxOccurs = ValueUtils.getValue(MaxOccursProperty.getInstance(), childType.getProperties());
				if (maxOccurs != null) {
					element.setProperty(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), maxOccurs));
				}
				structure.add(element);				
			}
			else {
				structure.add(new ComplexElementImpl((String) key, (ComplexType) childType, structure, new ValueImpl<Integer>(MinOccursProperty.getInstance(), required == null || !required.contains(key) ? 0 : 1)));
			}
		}
	}

	public boolean isAllowRemoteResolving() {
		return allowRemoteResolving;
	}

	public void setAllowRemoteResolving(boolean allowRemoteResolving) {
		this.allowRemoteResolving = allowRemoteResolving;
	}

	public TimeZone getTimezone() {
		return timezone;
	}

	public void setTimezone(TimeZone timezone) {
		this.timezone = timezone;
	}

}

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
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import be.nabu.libs.types.base.UUIDFormat;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.binding.api.Window;
import be.nabu.libs.types.binding.json.JSONBinding;
import be.nabu.libs.types.java.BeanResolver;
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
import be.nabu.libs.types.properties.UUIDFormatProperty;
import be.nabu.libs.types.structure.DefinedStructure;
import be.nabu.libs.validator.api.ValidationMessage;
import be.nabu.libs.validator.api.ValidationMessage.Severity;

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
	// we should be using big numbers by default when no specific format is set. however, this realisation came too late :(
	private boolean useDefaultBigNumbers = false;
	
	// you can set a type mapping where types will be added with a different name than the definition
	// the key is the "original" name as it occurs in the swagger, the value is the new name
	private Map<String, String> typeMapping;
	
	private TimeZone timezone;
	private boolean allowRemoteResolving = false;
	private List<SwaggerSecuritySetting> globalSecurity;
	private List<String> missingRefs = new ArrayList<String>();
	// too many swaggers are invalid, let's attempt to support them. for example if types are not defined, use java.lang.Object as a fallback
	private boolean allowInvalidSwagger = true;
	// prepulate complex types to allow for circular references
	private boolean usePrepulation = true;
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	// you can set a base id for the types
	private String typeBase;
	private UUIDFormat uuidFormat;
	
	// for some reason some people generate as swagger where the format is set to "uuid" but it is actually not a valid uuid...
	// for instance sendgrid for some reason uses a valid uuid but prepends it with "d-" making it invalid...
	private boolean allowUuid = true;
	
	// whether or not we want to cleanup the type names
	// this doesn't work, type registries require unique namespace + name combinations
	// we can set the type registry to use ids when possible (instead of the name) but than the resolving no longer works, as it _does_ generally search by actual name instead of id
	private boolean cleanupTypeNames = false;
	
	public static void main(String...args) throws IOException {
		URL url = new URL("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v2.0/json/petstore.json");
		url = new URL("https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/examples/v2.0/json/petstore-expanded.json");
		url = new URL("file:/home/alex/files/repository-nabu/testApplication/process/zoekenV2/swagger.json");
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
		return parseJson(input);
	}

	public static MapContent parseJson(InputStream input) {
		try {
			JSONBinding binding = new JSONBinding(new MapTypeGenerator(true), Charset.forName("UTF-8"));
			// we are not interested in comment sections that don't follow decent structures
			// regular swagger should be properly structured
			binding.setIgnoreInconsistentTypes(true);
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
		MapContent content = parseJson(id, input);
		return parse(id, content);
	}
	
	public SwaggerDefinition parse(String id, MapContent content) {
		try {
			SwaggerDefinitionImpl definition = new SwaggerDefinitionImpl(id);
			List<ValidationMessage> validate = validate(content);
			if (!validate.isEmpty() && !allowInvalidSwagger) {
				throw new IllegalArgumentException("The swagger is invalid: " + validate);
			}
			else {
				definition.setValidationMessages(validate);
			}
			if (allowRemoteResolving) {
				resolveRemoteRefs(content);
			}
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
	
	private List<ValidationMessage> validate(MapContent root) {
		missingRefs.clear();
		Map map = root.getContent();
		List<ValidationMessage> messages = new ArrayList<ValidationMessage>();
		validate(root, root, messages);
		return messages;
	}
	
	private void validate(MapContent root, MapContent current, List<ValidationMessage> messages) {
		Map map = current.getContent();
		MapContent definitions = (MapContent) root.get("definitions");
		for (Object key : map.entrySet()) {
			Map.Entry entry = ((Map.Entry) key);
			if (entry.getKey().equals("$ref")) {
				String name = entry.getValue().toString();
				if (name.startsWith("#/definitions/")) {
					name = name.substring("#/definitions/".length());
				}
				if (definitions == null || definitions.get(name) == null) {
					messages.add(new ValidationMessage(Severity.ERROR, "Could not resolve reference: " + entry.getValue()));
					missingRefs.add(name);
				}
			}
			else if (entry.getValue() instanceof MapContent) {
				validate(root, (MapContent) entry.getValue(), messages);
			}
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
			// extensions at this level, usually for documentation etc
			else if (method.toString().indexOf("x-") == 0) {
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
		else if ("3.0.0".equals(content.get("openapi")) || "3.0.1".equals(content.get("openapi"))) {
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
		TypeRegistryImpl registry = new TypeRegistryImpl();
		if (cleanupTypeNames) {
			registry.setUseTypeIds(true);
		}
		definition.setRegistry(registry);
		if (usePrepulation) {
			prepopulateTypes(definition, content);
		}
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
	
	public static void parseJsonSchema(InputStream input) {
		MapContent parseJson = parseJson(input);
	}
	
	public static boolean isValid(char character, boolean first, boolean last, boolean allowDots) {
		if (!allowDots && character == '.') {
			return false;
		}
		// dots are also allowed for namespacing
		return (!first && character >= 48 && character <= 57) || (!first && !last && character == 46) || (character >= 65 && character <= 90) || (character >= 97 && character <= 122);
	}
	
	public static void cleanupOperationId(SwaggerPath path, SwaggerMethodImpl method) {
		method.setOperationId(SwaggerParser.cleanup(method.getOperationId() == null ? method.getMethod() + path.getPath(): method.getOperationId()));
	}
	
	public static String cleanupOperationId(String path, String method, String operationId) {
		return cleanup(operationId == null ? method + path : operationId);
	}
	
	private String cleanupType(String name) {
		// first map it!
		name = mapTypeName(name);
		String cleanup = cleanup(name);
		if (typeBase != null && cleanup.startsWith(typeBase)) {
			cleanup = cleanup.substring(typeBase.length());
			// if you are cleaning up a "." path, don't start the remainder with one if you forgot to add it to the typebase
			if (cleanup.startsWith(".")) {
				cleanup = cleanup.substring(1);
			}
		}
		return cleanup;
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
	
	private String mapTypeName(String name) {
		return typeMapping != null && typeMapping.get(name) != null ? typeMapping.get(name) : name;
	}
	
	private Type findType(SwaggerDefinition definition, String name, Map<String, Type> ongoing) throws ParseException {
		if (name.startsWith("#/definitions/")) {
			name = name.substring("#/definitions/".length());
		}
		String originalName = name;
		name = cleanupType(name);
		
		Type type = definition.getRegistry().getComplexType(definition.getId(), name);
		if (type == null) {
			type = definition.getRegistry().getSimpleType(definition.getId(), name);
		}
		if (type == null && ongoing.containsKey(originalName)) {
			return ongoing.get(originalName);
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
			// if it was listed as missing, don't bother retrying
			// we can either not list it at all, or we return java.lang.Object, allowing for freestyle stuff...
			if (missingRefs.contains(name) && allowInvalidSwagger) {
				type = BeanResolver.getInstance().resolve(Object.class);
			}
			// throw an exception to retry later, it can be an ordering issue
			else {
				throw new ParseException("Can not resolve type: " + name, 1);
			}
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
			else if (schema.get("type") != null || schema.get("properties") != null || schema.get("allOf") != null || schema.get("anyOf") != null || schema.get("oneOf") != null) {
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
			List<Value> values = new ArrayList<Value>(Arrays.asList(new ValueImpl(MinOccursProperty.getInstance(), required == null || !required ? 0 : 1)));
			if (UUID.class.isAssignableFrom(((SimpleType) type).getInstanceClass()) && uuidFormat != null) {
				values.add(new ValueImpl<UUIDFormat>(UUIDFormatProperty.getInstance(), uuidFormat));
			}
			return new SimpleElementImpl(name, (SimpleType<?>) type, null, values.toArray(new Value[values.size()]));
		}
		else {
			return new ComplexElementImpl(name, (ComplexType) type, null, new ValueImpl(MinOccursProperty.getInstance(), required == null || !required ? 0 : 1));
		}
	}
	
	private String cleanupTypeName(String name) {
		// if the name contains dots, we assume the part after the last dot is the real name and the rest is just hierarchy
		// the hierarchy might be important, informative and necessary to make it unique (e.g. for our own types)
		// but the name, as it is set in the type, is not required to be unique, but rather informative
		// so we focus on that last part
		// we do this with a specific boolean though, cause we need to update type registry behavior to match
		// by default type registries rely on namespace + name to be unique, not the id
		if (!cleanupTypeNames) {
			return name;
		}
		int lastIndexOf = name.lastIndexOf('.');
		if (lastIndexOf > 0 && lastIndexOf < name.length() - 1) {
			return name.substring(lastIndexOf + 1);
		}
		else {
			return name;
		}
	}
	
	// to allow for circular references, we add an empty definition at the root, a placeholder
	// TODO: currently we don't prepopulate the simple types and arrays
	// simple types likely won't have circular references
	// arrays might, but root arrays are a rare thing, and we need more resolving to set a proper type
	// for now, we support the biggest usecase: complex types with circular references
	private void prepopulateTypes(SwaggerDefinition definition, MapContent root) {
		MapContent definitions = (MapContent) root.get("definitions");
		if (definitions != null) {
			for (Object key : definitions.getContent().entrySet()) {
				try {
					Map.Entry entry = ((Map.Entry) key);
					MapContent content = (MapContent) entry.getValue();
					Object object = content.get("type");
					/**
					 * The sendgrid swagger contained 47 instances of:
					 *  "type": [
                            "null",
                            "string"
                        ],
                        
                        while not allowed by the spec, there does not seem a downside in adding (dubious) support for this
					 */
					if (object instanceof List) {
						for (Object single : (List) object) {
							if (single != null && !"null".equals(single)) {
								object = single;
								break;
							}
						}
					}
					// end of sendgrid "fix"
					
					String type = (String) object;
					
					ModifiableType result = null;
					if (type == null || type.equals("object")) {
						String cleanedUpName = cleanupType(entry.getKey().toString());
						String typeId = definition.getId() + ".types." + cleanedUpName;
						DefinedStructure structure = new DefinedStructure();
						structure.setNamespace(definition.getId());
						structure.setName(cleanupTypeName(cleanedUpName));
						structure.setId(typeId);
						result = structure;
					}
					// because of type mapping, we may force a type to exist twice
					// for example we had an external party that was updating its type structure piece by piece, specifically moving stuff around
					// in the transition releases between the original and the ultimate refactor, they released multiple versions where the same type existed twice: in the old location and the new
					// we can force them to be the same but they can not be registered twice
					if (result instanceof SimpleType) {
						SimpleType<?> simpleType = definition.getRegistry().getSimpleType(result.getNamespace(), result.getName());
						if (simpleType == null) {
							((ModifiableTypeRegistry) definition.getRegistry()).register((SimpleType<?>) result);
						}
					}
					else if (result instanceof ComplexType) {
						ComplexType complexType = definition.getRegistry().getComplexType(result.getNamespace(), result.getName());
						if (complexType == null) {
							((ModifiableTypeRegistry) definition.getRegistry()).register((ComplexType) result);
						}
					}
				}
				catch (Exception e) {
					logger.error("Could not load definition for: " + key, e);
					throw new RuntimeException(e);
				}
			}
		}
	}
	
	// we can't expose inline simple types as defined because you might have a lot with the same name and different (or even the same) values, the name is only the local element
	// the ongoing allows for circular references to oneself
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Type parseDefinedType(SwaggerDefinition definition, String name, Map<String, Object> content, boolean isRoot, boolean checkUndefinedRequired, Map<String, Type> ongoing) throws ParseException {
		Object object = content.get("type");
		/**
		 * The sendgrid swagger contained 47 instances of:
		 *  "type": [
                "null",
                "string"
            ],
            
            while not allowed by the spec, there does not seem a downside in adding (dubious) support for this
		 */
		if (object instanceof List) {
			for (Object single : (List) object) {
				if (single != null && !"null".equals(single)) {
					object = single;
					break;
				}
			}
		}
		// end of sendgrid "fix"
		
		String type = (String) object;
		
		String cleanedUpName = cleanupType(name);
		// make sure we use the cleaned up (and potentially renamed!) version
		name = cleanedUpName;
		String typeId = definition.getId() + ".types." + cleanedUpName;
		List<Value<?>> values = new ArrayList<Value<?>>();

		boolean alreadyRegistered = false;
		ModifiableType result;
		// complex type
		if (type == null || type.equals("object")) {
			// we resolve from the registry just in case we registered it before through prepopulation
			DefinedStructure structure;
			
			if (isRoot && definition.getRegistry().getComplexType(definition.getId(), cleanupTypeNames ? typeId : name) != null) {
				structure = (DefinedStructure) definition.getRegistry().getComplexType(definition.getId(), cleanupTypeNames ? typeId : name);
				alreadyRegistered = true;
			}
			else {
				structure = new DefinedStructure();
				if (isRoot) {
					structure.setNamespace(definition.getId());
				}
				structure.setName(cleanupTypeName(name));
				structure.setId(typeId);
			}
			
			ongoing.put(name, structure);
			
			// allows for composition, it seems that multiple inheritance is possible which we do not support
			if (content.get("allOf") != null || content.get("oneOf") != null || content.get("anyOf") != null) {
				List<Object> allOf = (List<Object>) content.get("allOf");
				if (allOf == null) {
					allOf = (List<Object>) content.get("oneOf");
				}
				if (allOf == null) {
					allOf = (List<Object>) content.get("anyOf");
				}
				// first find the root we are extending (if any)
				for (Object single : allOf) {
					Map<String, Object> singleMap = ((MapContent) single).getContent();
					if (singleMap.containsKey("$ref")) {
						// IMPORTANT: when you have an allOf (or whatever) of type A and B
						// the first type (A) will be set as supertype and B will be copied
						// however if B is not yet properly parsed it will exist by reference but be empty
						// we should retry parsing at a later point
						// however at that point A is already set as supertype, check openapi implementation for similar details
						
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
									// don't readd
									if (structure.get(child.getName()) == null) {
										if (child.getType() instanceof ComplexType) {
											structure.add(new ComplexElementImpl(child.getName(), (ComplexType) child.getType(), structure, child.getProperties()));
										}
										else {
											structure.add(new SimpleElementImpl(child.getName(), (SimpleType<?>) child.getType(), structure, child.getProperties()));
										}
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
						if (((MapContent) single).get("properties") != null) {
							parseStructureProperties(definition, name, ((MapContent) single).getContent(), structure, (MapContent) ((MapContent) single).get("properties"), ongoing);
						}
						else {
							logger.warn("Could not find $ref or properties for allOf " + name);
						}
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
					structure.setName(cleanupTypeName(name));
					structure.setId(typeId);
				}
				else {
					structure.setName(cleanupTypeName(name));
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
				else if (useDefaultBigNumbers) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(BigDecimal.class);
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
				else if (useDefaultBigNumbers) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(BigInteger.class);
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
				else if (format.equals("uuid") && allowUuid) {
					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(UUID.class);
					if (uuidFormat != null) {
						values.add(new ValueImpl<UUIDFormat>(UUIDFormatProperty.getInstance(), uuidFormat));	
					}
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
		
		if (isRoot && !alreadyRegistered) {
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
		if (properties.getContent() != null) {
			for (Object key : properties.getContent().keySet()) {
				MapContent propertyContent = (MapContent) properties.getContent().get(key);
				String reference = (String) propertyContent.get("$ref");
				// we have had instances where both $ref and type were present, in this case they were the same, for example:
				/*
				        "properties": {
					        "typeId": {
					          "$ref": "string",
					          "type": "string",
					          "example": "00000000-0000-0000-0000-000000000000"
					        }
					     }
				 */
				if (reference != null && reference.equals(propertyContent.get("type"))) {
					reference = null;
				}
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
		else {
			logger.warn("Empty properties found for: " + name);
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

	public String getTypeBase() {
		return typeBase;
	}

	public void setTypeBase(String typeBase) {
		this.typeBase = typeBase;
	}

	public UUIDFormat getUuidFormat() {
		return uuidFormat;
	}

	public void setUuidFormat(UUIDFormat uuidFormat) {
		this.uuidFormat = uuidFormat;
	}

	public boolean isAllowUuid() {
		return allowUuid;
	}

	public void setAllowUuid(boolean allowUuid) {
		this.allowUuid = allowUuid;
	}

	public Map<String, String> getTypeMapping() {
		return typeMapping;
	}

	public void setTypeMapping(Map<String, String> typeMapping) {
		this.typeMapping = typeMapping;
	}

	public boolean isUseDefaultBigNumbers() {
		return useDefaultBigNumbers;
	}

	public void setUseDefaultBigNumbers(boolean useDefaultBigNumbers) {
		this.useDefaultBigNumbers = useDefaultBigNumbers;
	}
}

//package be.nabu.libs.swagger.parser;
//
//import java.io.InputStream;
//import java.math.BigDecimal;
//import java.math.BigInteger;
//import java.net.URI;
//import java.net.URISyntaxException;
//import java.text.ParseException;
//import java.util.ArrayList;
//import java.util.Collection;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.TimeZone;
//import java.util.UUID;
//
//import be.nabu.libs.converter.ConverterFactory;
//import be.nabu.libs.property.api.Value;
//import be.nabu.libs.resources.URIUtils;
//import be.nabu.libs.resources.api.ResourceContainer;
//import be.nabu.libs.swagger.api.SwaggerDefinition;
//import be.nabu.libs.types.SimpleTypeWrapperFactory;
//import be.nabu.libs.types.TypeRegistryImpl;
//import be.nabu.libs.types.TypeUtils;
//import be.nabu.libs.types.api.ComplexType;
//import be.nabu.libs.types.api.DefinedType;
//import be.nabu.libs.types.api.Element;
//import be.nabu.libs.types.api.Marshallable;
//import be.nabu.libs.types.api.ModifiableType;
//import be.nabu.libs.types.api.ModifiableTypeRegistry;
//import be.nabu.libs.types.api.SimpleType;
//import be.nabu.libs.types.api.Type;
//import be.nabu.libs.types.api.TypeRegistry;
//import be.nabu.libs.types.base.ComplexElementImpl;
//import be.nabu.libs.types.base.SimpleElementImpl;
//import be.nabu.libs.types.base.UUIDFormat;
//import be.nabu.libs.types.base.ValueImpl;
//import be.nabu.libs.types.java.BeanResolver;
//import be.nabu.libs.types.java.BeanType;
//import be.nabu.libs.types.map.MapContent;
//import be.nabu.libs.types.properties.CommentProperty;
//import be.nabu.libs.types.properties.EnumerationProperty;
//import be.nabu.libs.types.properties.FormatProperty;
//import be.nabu.libs.types.properties.MaxExclusiveProperty;
//import be.nabu.libs.types.properties.MaxInclusiveProperty;
//import be.nabu.libs.types.properties.MaxLengthProperty;
//import be.nabu.libs.types.properties.MaxOccursProperty;
//import be.nabu.libs.types.properties.MinExclusiveProperty;
//import be.nabu.libs.types.properties.MinInclusiveProperty;
//import be.nabu.libs.types.properties.MinLengthProperty;
//import be.nabu.libs.types.properties.MinOccursProperty;
//import be.nabu.libs.types.properties.PatternProperty;
//import be.nabu.libs.types.properties.TimezoneProperty;
//import be.nabu.libs.types.properties.UUIDFormatProperty;
//import be.nabu.libs.types.structure.DefinedStructure;
//
//public class JSONSchemaParser {
//	
//	private String idBase;
//	private boolean cleanupTypeNames;
//	private boolean allowRemoteResolving;
//	
//	// remote resources are cached here, both for performance reasons and stability (so that remote links don't disappear)
//	// if no cache container is configured, it will always be loaded remotely
//	private ResourceContainer<?> cacheContainer;
//	
//	// the json schema itself is the default registry (key = null)
//	// it can import other schema's which are referenced by their unique URL
//	private Map<String, TypeRegistry> registries = new HashMap<String, TypeRegistry>();
//	
//	public JSONSchemaParser(String idBase) {
//		this.idBase = idBase;
//	}
//	
//	private TypeRegistry getDefaultRegistry() {
//		return getRegistry(getDefaultNamespace());
//	}
//	
//	private String getDefaultNamespace() {
//		return null;
//	}
//	
//	private TypeRegistry getRegistry(String name) {
//		if (registries.get(name) == null) {
//			registries.put(name, new TypeRegistryImpl());
//		}
//		return registries.get(name);
//	}
//	
//	// to allow for circular references, we add an empty definition at the root, a placeholder
//	// TODO: currently we don't prepopulate the simple types and arrays
//	// simple types likely won't have circular references
//	// arrays might, but root arrays are a rare thing, and we need more resolving to set a proper type
//	// for now, we support the biggest usecase: complex types with circular references
//	private void prepopulateTypes(SwaggerDefinition definition, MapContent root) {
//		MapContent definitions = (MapContent) root.get("definitions");
//		if (definitions != null) {
//			for (Object key : definitions.getContent().entrySet()) {
//				Map.Entry entry = ((Map.Entry) key);
//				MapContent content = (MapContent) entry.getValue();
//				String type = (String) content.get("type");
//				
//				ModifiableType result = null;
//				if (type == null || type.equals("object")) {
//					String cleanedUpName = cleanupType(entry.getKey().toString());
//					String typeId = definition.getId() + ".types." + cleanedUpName;
//					DefinedStructure structure = new DefinedStructure();
//					structure.setNamespace(definition.getId());
//					structure.setName(cleanupTypeName(entry.getKey().toString()));
//					structure.setId(typeId);
//					result = structure;
//				}
//				if (result instanceof SimpleType) {
//					((ModifiableTypeRegistry) definition.getRegistry()).register((SimpleType<?>) result);
//				}
//				else if (result instanceof ComplexType) {
//					((ModifiableTypeRegistry) definition.getRegistry()).register((ComplexType) result);
//				}
//			}
//		}
//	}
//
//	private void parseDefinitions(MapContent content) throws ParseException {
//		TypeRegistryImpl registry = new TypeRegistryImpl();
//		if (cleanupTypeNames) {
//			registry.setUseTypeIds(true);
//		}
//		definition.setRegistry(registry);
//		prepopulateTypes(definition, content);
//		
//		MapContent definitions = (MapContent) content.get("definitions");
//		if (definitions != null) {
//			List<String> previousFailed = null;
//			List<String> failed = null;
//			while (previousFailed == null || failed.size() < previousFailed.size()) {
//				Collection<?> toParse = failed == null ? definitions.getContent().keySet() : failed;
//				previousFailed = failed;
//				failed = new ArrayList<String>();
//				for (Object typeName : toParse) {
//					try {
//						parseDefinedType(definition, (String) typeName, ((MapContent) definitions.getContent().get(typeName)).getContent(), true, false, new HashMap<String, Type>());
//					}
//					catch (ParseException e) {
//						// we should repeat
//						if (e.getErrorOffset() == 1) {
//							failed.add((String) typeName);
//						}
//						else {
//							throw e;
//						}
//					}
//				}
//			}
//			if (failed != null && !failed.isEmpty()) {
//				throw new ParseException("Could not parse all the elements: " + failed, 0);
//			}
//		}
//	}
//	
//	private Type findType(String ref, Map<String, Type> ongoing) throws ParseException {
//		if (ref.startsWith("#/definitions/")) {
//			return findType(getDefaultNamespace(), ref.substring("#/definitions/".length()), ongoing);
//		}
//		else {
//			try {
//				URI uri = new URI(URIUtils.encodeURI(ref));
//				if (uri.getHost() != null) {
//					if (allowRemoteResolving) {
//						try {
//							InputStream stream = uri.toURL().openStream();
//							try {
//								MapContent parsed = SwaggerParser.parseJson(stream);
//								
//							}
//							finally {
//								stream.close();
//							}
//						}
//						catch (Exception e) {
//							throw new ParseException("Can not load external ref '" + ref + "': " + e.getMessage(), 2);
//						}
//					}
//					else {
//						throw new ParseException("Remote ref found '" + ref + "', this is currently disabled", 2);		
//					}
//				}
//				else {
//					throw new ParseException("Unknown ref found '" + ref + "', it does not appear to be a valid reference", 2);
//				}
//			}
//			catch (URISyntaxException e) {
//				throw new ParseException("Invalid ref '" + ref + "': " + e.getMessage(), 2);
//			}
//		}
//	}
//	
//	private Type findType(String namespace, String name, Map<String, Type> ongoing) throws ParseException {
//		TypeRegistry registry = getRegistry(namespace);
//		if (name.startsWith("#/definitions/")) {
//			name = name.substring("#/definitions/".length());
//		}
//		Type type = registry.getComplexType(namespace, name);
//		if (type == null) {
//			type = registry.getSimpleType(namespace, name);
//		}
//		if (type == null && ongoing.containsKey(name)) {
//			return ongoing.get(name);
//		}
//		if (type == null) {
//			name = SwaggerParser.cleanup(name);
//			type = registry.getComplexType(namespace, name);
//			if (type == null) {
//				type = registry.getSimpleType(namespace, name);
//			}
//			if (type == null && ongoing.containsKey(name)) {
//				return ongoing.get(name);
//			}
//		}
//		if (type == null) {
//			throw new ParseException("Can not resolve type: " + name, 1);
//		}
//		return type;
//	}
//	
//	
//	// we can't expose inline simple types as defined because you might have a lot with the same name and different (or even the same) values, the name is only the local element
//	// the ongoing allows for circular references to oneself
//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	private Type parseDefinedType(String name, Map<String, Object> content, boolean isRoot, boolean checkUndefinedRequired, Map<String, Type> ongoing) throws ParseException {
//		String type = (String) content.get("type");
//		
//		String cleanedUpName = SwaggerParser.cleanup(name);
//		String typeId = idBase + ".types." + cleanedUpName;
//		List<Value<?>> values = new ArrayList<Value<?>>();
//
//		boolean alreadyRegistered = false;
//		ModifiableType result;
//		// complex type
//		if (type == null || type.equals("object")) {
//			// we resolve from the registry just in case we registered it before through prepopulation
//			DefinedStructure structure;
//			
//			if (isRoot && getDefaultRegistry().getComplexType(getDefaultNamespace(), cleanupTypeNames ? typeId : name) != null) {
//				structure = (DefinedStructure) getDefaultRegistry().getComplexType(getDefaultNamespace(), cleanupTypeNames ? typeId : name);
//				alreadyRegistered = true;
//			}
//			else {
//				structure = new DefinedStructure();
//				if (isRoot) {
//					structure.setNamespace(getDefaultNamespace());
//				}
//				structure.setName(name);
//				structure.setId(typeId);
//			}
//			
//			ongoing.put(name, structure);
//			
//			// allows for composition, it seems that multiple inheritance is possible which we do not support
//			if (content.get("allOf") != null) {
//				List<Object> allOf = (List<Object>) content.get("allOf");
//				// first find the root we are extending (if any)
//				for (Object single : allOf) {
//					Map<String, Object> singleMap = ((MapContent) single).getContent();
//					if (singleMap.containsKey("$ref")) {
//						// the first ref will be mapped as a supertype
//						if (structure.getSuperType() == null) {
//							Type superType = findType(definition, (String) singleMap.get("$ref"), ongoing);
//							if (superType == null) {
//								throw new ParseException("Can not find super type: " + singleMap.get("$ref"), 1);
//							}
//							structure.setSuperType(superType);
//						}
//						// other refs are expanded in it
//						else {
//							Type superType = findType(definition, (String) singleMap.get("$ref"), ongoing);
//							if (superType == null) {
//								throw new ParseException("Can not find super type: " + singleMap.get("$ref"), 1);
//							}
//							if (superType instanceof ComplexType) {
//								for (Element<?> child : TypeUtils.getAllChildren((ComplexType) superType)) {
//									if (child.getType() instanceof ComplexType) {
//										structure.add(new ComplexElementImpl(child.getName(), (ComplexType) child.getType(), structure, child.getProperties()));
//									}
//									else {
//										structure.add(new SimpleElementImpl(child.getName(), (SimpleType<?>) child.getType(), structure, child.getProperties()));
//									}
//								}
//							}
//							else {
//								throw new ParseException("Can only unfold a complex type when doing multiple extensions", 2);
//							}
//						}
//					}
//				}
//				// find all none-reference extensions
//				for (Object single : allOf) {
//					Map<String, Object> singleMap = ((MapContent) single).getContent();
//					if (!singleMap.containsKey("$ref")) {
//						parseStructureProperties(definition, name, ((MapContent) single).getContent(), structure, (MapContent) ((MapContent) single).get("properties"), ongoing);
//					}
//				}
//			}
//			else {
//				MapContent properties = (MapContent) content.get("properties");
//				if (properties != null) {
//					parseStructureProperties(definition, name, content, structure, properties, ongoing);
//				}
//			}
//			
//			// if the structure has no fields, make it a generic object instead (unless it is a root, because then it is referred to by other types and must be resolvable)
//			if (!isRoot && TypeUtils.getAllChildren(structure).isEmpty()) {
//				result = new BeanType<Object>(Object.class);
//			}
//			else {
//				result = structure;
//			}
//		}
//		else if (type.equals("array")) {
//			// the actual type is in "items"
//			MapContent items = (MapContent) content.get("items");
//			
//			Type parsedDefinedType;
//			
//			// if no items are specified, we assume a string array
//			if (items == null) {
//			//	throw new ParseException("Found an array instance without an items definition for: " + name, 0);
//				parsedDefinedType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
//			}
//			else {
//				parsedDefinedType = items.get("$ref") == null 
//					? parseDefinedType(definition, name, items.getContent(), false, checkUndefinedRequired, ongoing)
//					: findType(definition, (String) items.get("$ref"), ongoing);
//			}
//			
//			// we need to extend it to add the fucked up max/min occurs properties...
//			// this extension does not need to be registered globally (in general)
//			// nabu allows for casting in parents to children, so at runtime you can create a parent instance and cast it to the child
//			// so this will work transparently...
//			if (parsedDefinedType instanceof Marshallable) {
//				result = new MarshallableSimpleTypeExtension(
//					!isRoot && parsedDefinedType instanceof DefinedType ? ((DefinedType) parsedDefinedType).getId() : typeId, 
//					isRoot ? definition.getId() : null, 
//					name, 
//					(SimpleType) parsedDefinedType
//				);
//			}
//			else if (parsedDefinedType instanceof SimpleType) {
//				result = new SimpleTypeExtension(
//					!isRoot && parsedDefinedType instanceof DefinedType ? ((DefinedType) parsedDefinedType).getId() : typeId, 
//					isRoot ? definition.getId() : null, 
//					name, 
//					(SimpleType) parsedDefinedType
//				);
//			}
//			else {
//				DefinedStructure structure = new DefinedStructure();
//				structure.setSuperType(parsedDefinedType);
//				if (isRoot) {
//					structure.setNamespace(definition.getId());
//					structure.setName(cleanupTypeName(name));
//					structure.setId(typeId);
//				}
//				else {
//					structure.setName(cleanupTypeName(name));
//					structure.setId(parsedDefinedType instanceof DefinedType ? ((DefinedType) parsedDefinedType).getId() : typeId);
//				}
////				if (isRoot && parsedDefinedType instanceof DefinedType && ((DefinedType) parsedDefinedType).getId().equals(structure.getId())) {
////					throw new ParseException("No unique naming: " + structure.getId(), 0);
////				}
//				result = structure;
//			}
//			
//			Number maxOccurs = (Number) content.get("maxItems");
//			Number minOccurs = (Number) content.get("minItems");
//			
//			int defaultMaxOccurs = result instanceof SimpleType && ((SimpleType<?>) result).getInstanceClass().equals(byte[].class) ? 1 : 0;
//			values.add(new ValueImpl<Integer>(MaxOccursProperty.getInstance(), maxOccurs == null ? defaultMaxOccurs : maxOccurs.intValue()));
//			values.add(new ValueImpl<Integer>(MinOccursProperty.getInstance(), minOccurs == null ? 0 : minOccurs.intValue()));
//		}
//		// simple type
//		else {
//			SimpleType<?> simpleType;
//			// note that the "format" is only indicative, there are a few listed formats you should use
//			// but apart from that you can use any format you choose
//			String format = (String) content.get("format");
//			if (type.equals("number")) {
//				if (format == null || format.equals("double")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Double.class);
//				}
//				else if (format.equals("float")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Float.class);
//				}
//				else {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Double.class);
//					values.add(new ValueImpl<String>(CommentProperty.getInstance(), "Unsupported Format: " + format));
//				}
//			}
//			else if (type.equals("integer")) {
//				if (format == null || format.equals("int32")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Integer.class);
//				}
//				else if (format.equals("int64")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Long.class);
//				}
//				else if (format.equalsIgnoreCase("bigInteger")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(BigInteger.class);
//				}
//				else if (format.equalsIgnoreCase("bigDecimal")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(BigDecimal.class);
//				}
//				else {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Long.class);
//					values.add(new ValueImpl<String>(CommentProperty.getInstance(), "Unsupported Format: " + format));
//				}
//			}
//			else if (type.equals("boolean")) {
//				simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Boolean.class);
//			}
//			else if (type.equals("file")) {
//				simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(InputStream.class);
//			}
//			// we put all unrecognized types into a string as well to be lenient (e.g. the github swagger had a type "null", presumably an error in generation...)
//			else { // if (type.equals("string"))
//				if (!type.equals("string") || format == null || format.equals("string") || format.equals("password")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
//				}
//				// unofficial, added for digipolis
//				else if (format.equals("uri")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(URI.class);
//				}
//				else if (format.equals("byte")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(byte[].class);
//				}
//				else if (format.equals("binary")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(InputStream.class);
//				}
//				else if (format.equals("date")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Date.class);
//					values.add(new ValueImpl<String>(FormatProperty.getInstance(), "date"));
//					if (timezone != null) {
//						values.add(new ValueImpl<TimeZone>(TimezoneProperty.getInstance(), timezone));
//					}
//				}
//				// don't set any additional properties for dateTime, this is the default and we avoid generating some unnecessary simple types
//				else if (format.equals("date-time")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(Date.class);
//					if (timezone != null) {
//						values.add(new ValueImpl<TimeZone>(TimezoneProperty.getInstance(), timezone));
//					}
//				}
//				else if (format.equals("uuid")) {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(UUID.class);
//					if (uuidFormat != null) {
//						values.add(new ValueImpl<UUIDFormat>(UUIDFormatProperty.getInstance(), uuidFormat));	
//					}
//				}
//				else {
//					simpleType = SimpleTypeWrapperFactory.getInstance().getWrapper().wrap(String.class);
//					values.add(new ValueImpl<String>(CommentProperty.getInstance(), "Unsupported Format: " + format));
//				}
//			}
//			
//			Boolean required = (Boolean) content.get("required");
//			// the default value for required (false) is the opposite of the minoccurs
//			// if it is not specified, do we want it to be inserted?
//			if ((required == null && checkUndefinedRequired) || (required != null && !required)) {
//				values.add(new ValueImpl<Integer>(MinOccursProperty.getInstance(), 0));
//			}
//			
//			result = new MarshallableSimpleTypeExtension(typeId, isRoot ? definition.getId() : null, name, simpleType);
//		}
//		
//		if (isRoot && !alreadyRegistered) {
//			if (result instanceof SimpleType) {
//				((ModifiableTypeRegistry) definition.getRegistry()).register((SimpleType<?>) result);
//			}
//			else {
//				((ModifiableTypeRegistry) definition.getRegistry()).register((ComplexType) result);
//			}
//		}
//		
//		// common shit
//		String description = (String) content.get("description");
//		if (description != null) {
//			values.add(new ValueImpl<String>(CommentProperty.getInstance(), description));
//		}
//		Number maximum = (Number) content.get("maximum");
//		Boolean exclusiveMaximum = (Boolean) content.get("exclusiveMaximum");
//		if (maximum != null) {
//			Object convert = ConverterFactory.getInstance().getConverter().convert(maximum, ((SimpleType<?>) result).getInstanceClass());
//			values.add(new ValueImpl(exclusiveMaximum == null || !exclusiveMaximum ? new MaxInclusiveProperty() : new MaxExclusiveProperty(), convert));
//		}
//		
//		Number minimum = (Number) content.get("minimum");
//		Boolean exclusiveMinimum = (Boolean) content.get("exclusiveMinimum");
//		if (minimum != null) {
//			Object convert = ConverterFactory.getInstance().getConverter().convert(minimum, ((SimpleType<?>) result).getInstanceClass());
//			values.add(new ValueImpl(exclusiveMinimum == null || !exclusiveMinimum ? new MinInclusiveProperty() : new MinExclusiveProperty(), convert));
//		}
//		
//		Number maxLength = (Number) content.get("maxLength");
//		if (maxLength != null) {
//			values.add(new ValueImpl(MaxLengthProperty.getInstance(), maxLength.intValue()));
//		}
//		
//		Number minLength = (Number) content.get("minLength");
//		if (minLength != null) {
//			values.add(new ValueImpl(MinLengthProperty.getInstance(), minLength.intValue()));
//		}
//		
//		List<?> enumValues = (List<?>) content.get("enum");
//		if (enumValues != null && !enumValues.isEmpty()) {
//			values.add(new ValueImpl(new EnumerationProperty(), enumValues));
//		}
//		
//		String pattern = (String) content.get("pattern");
//		if (pattern != null) {
//			values.add(new ValueImpl(PatternProperty.getInstance(), pattern));
//		}
//		
//		// if we have a simple type with no additional settings and it is not a root definition, unwrap it to the original simple type
//		if (values.isEmpty() && !isRoot && result instanceof SimpleType) {
//			return result.getSuperType();
//		}
//		else {
//			result.setProperty(values.toArray(new Value[values.size()]));
//			return result;
//		}
//	}
//	
//}

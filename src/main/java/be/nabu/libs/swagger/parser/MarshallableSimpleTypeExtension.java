package be.nabu.libs.swagger.parser;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.Marshallable;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.api.Unmarshallable;
import be.nabu.libs.types.base.BaseMarshallableSimpleType;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.structure.SuperTypeProperty;

public class MarshallableSimpleTypeExtension<T> extends BaseMarshallableSimpleType<T> implements Unmarshallable<T> {

	private String id;
	private String namespace;
	private String name;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MarshallableSimpleTypeExtension(String id, String namespace, String name, SimpleType<T> superType) {
		super(superType.getInstanceClass());
		this.id = id;
		this.namespace = namespace;
		this.name = name;
		setSuperType(superType);
		setProperty(new ValueImpl(SuperTypeProperty.getInstance(), superType));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public String marshal(T object, Value<?>...values) {
		return ((Marshallable<T>) getSuperType()).marshal(object, values);
	}

	@Override
	public String getName(Value<?>...values) {
		return name;
	}

	@Override
	public String getNamespace(Value<?>...values) {
		return namespace;
	}

	@Override
	public String getId() {
		return id;
	}

	@SuppressWarnings("unchecked")
	@Override
	public T unmarshal(String content, Value<?>...values) {
		return ((Unmarshallable<T>) getSuperType()).unmarshal(content, values);
	}
	
}

package be.nabu.libs.swagger.parser;

import be.nabu.libs.property.api.Value;
import be.nabu.libs.types.api.SimpleType;
import be.nabu.libs.types.base.BaseSimpleType;
import be.nabu.libs.types.base.ValueImpl;
import be.nabu.libs.types.structure.SuperTypeProperty;

public class SimpleTypeExtension<T> extends BaseSimpleType<T> {

	private String id;
	private String namespace;
	private String name;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public SimpleTypeExtension(String id, String namespace, String name, SimpleType<T> superType) {
		super(superType.getInstanceClass());
		this.id = id;
		this.namespace = namespace;
		this.name = name;
		setSuperType(superType);
		setProperty(new ValueImpl(SuperTypeProperty.getInstance(), superType));
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
	
}

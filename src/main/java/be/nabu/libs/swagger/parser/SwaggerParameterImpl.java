package be.nabu.libs.swagger.parser;

import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.types.api.Element;
import be.nabu.libs.types.base.CollectionFormat;

public class SwaggerParameterImpl implements SwaggerParameter {
	
	private Number multipleOf;
	private Boolean unique, allowEmptyValue;
	private Object defaultValue;
	private ParameterLocation location;
	private Element<?> element;
	private CollectionFormat collectionFormat;
	private String name;
	
	@Override
	public ParameterLocation getLocation() {
		return location;
	}
	public void setLocation(ParameterLocation location) {
		this.location = location;
	}
	
	@Override
	public Element<?> getElement() {
		return element;
	}
	public void setElement(Element<?> element) {
		this.element = element;
	}
	
	@Override
	public Number getMultipleOf() {
		return multipleOf;
	}
	public void setMultipleOf(Number multipleOf) {
		this.multipleOf = multipleOf;
	}
	
	@Override
	public Boolean getUnique() {
		return unique;
	}
	public void setUnique(Boolean unique) {
		this.unique = unique;
	}
	
	@Override
	public Boolean getAllowEmptyValue() {
		return allowEmptyValue;
	}
	public void setAllowEmptyValue(Boolean allowEmptyValue) {
		this.allowEmptyValue = allowEmptyValue;
	}

	@Override
	public Object getDefaultValue() {
		return defaultValue;
	}
	public void setDefaultValue(Object defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	@Override
	public CollectionFormat getCollectionFormat() {
		return collectionFormat;
	}
	public void setCollectionFormat(CollectionFormat collectionFormat) {
		this.collectionFormat = collectionFormat;
	}
	
	@Override
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
}

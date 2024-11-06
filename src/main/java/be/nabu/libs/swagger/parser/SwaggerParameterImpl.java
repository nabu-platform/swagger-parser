/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
	private String description;
	private boolean explode, allowReserved;
	
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
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public boolean isExplode() {
		return explode;
	}
	public void setExplode(boolean explode) {
		this.explode = explode;
	}
	
	@Override
	public boolean isAllowReserved() {
		return allowReserved;
	}
	public void setAllowReserved(boolean allowReserved) {
		this.allowReserved = allowReserved;
	}
	
}

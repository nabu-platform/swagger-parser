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

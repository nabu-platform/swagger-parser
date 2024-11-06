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

import java.util.List;

import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.types.api.Element;

public class SwaggerResponseImpl implements SwaggerResponse {

	private Integer code;
	private String description;
	private Element<?> element;
	private List<SwaggerParameter> headers;
	private List<String> produces;
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public Element<?> getElement() {
		return element;
	}
	public void setElement(Element<?> element) {
		this.element = element;
	}
	
	@Override
	public List<SwaggerParameter> getHeaders() {
		return headers;
	}
	public void setHeaders(List<SwaggerParameter> headers) {
		this.headers = headers;
	}
	
	@Override
	public Integer getCode() {
		return code;
	}
	public void setCode(Integer code) {
		this.code = code;
	}
	
	@Override
	public List<String> getProduces() {
		return produces;
	}
	public void setProduces(List<String> produces) {
		this.produces = produces;
	}

	
}

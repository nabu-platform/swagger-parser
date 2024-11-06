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
import java.util.Map;

import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerPath;

public class SwaggerPathImpl implements SwaggerPath {
	
	private String path;
	private List<SwaggerMethod> methods;
	private Map<String, SwaggerParameter> parameters;
	private String description, summary;
	
	@Override
	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	
	@Override
	public List<SwaggerMethod> getMethods() {
		return methods;
	}
	public void setMethods(List<SwaggerMethod> methods) {
		this.methods = methods;
	}

	public Map<String, SwaggerParameter> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, SwaggerParameter> parameters) {
		this.parameters = parameters;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}

	@Override
	public String toString() {
		return path + methods;
	}
	
}

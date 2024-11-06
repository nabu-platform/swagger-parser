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

import be.nabu.libs.swagger.api.SwaggerDocumentation;
import be.nabu.libs.swagger.api.SwaggerMethod;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.swagger.api.SwaggerSecuritySetting;

public class SwaggerMethodImpl implements SwaggerMethod {
	
	private String method, summary, description, operationId;
	private List<String> consumes, produces, tags, schemes;
	private SwaggerDocumentation documentation;
	private List<SwaggerParameter> parameters;
	private Boolean deprecated;
	private List<SwaggerResponse> responses;
	private List<SwaggerSecuritySetting> security;
	private Map<String, Object> extensions;
	
	@Override
	public String getMethod() {
		return method;
	}
	public void setMethod(String method) {
		this.method = method;
	}
	
	@Override
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String getOperationId() {
		return operationId;
	}
	public void setOperationId(String operationId) {
		this.operationId = operationId;
	}

	@Override
	public List<String> getConsumes() {
		return consumes;
	}
	public void setConsumes(List<String> consumes) {
		this.consumes = consumes;
	}
	
	@Override
	public List<String> getProduces() {
		return produces;
	}
	public void setProduces(List<String> produces) {
		this.produces = produces;
	}
	
	@Override
	public List<String> getTags() {
		return tags;
	}
	public void setTags(List<String> tags) {
		this.tags = tags;
	}
	
	@Override
	public SwaggerDocumentation getDocumentation() {
		return documentation;
	}
	public void setDocumentation(SwaggerDocumentation documentation) {
		this.documentation = documentation;
	}
	
	@Override
	public List<SwaggerParameter> getParameters() {
		return parameters;
	}
	public void setParameters(List<SwaggerParameter> parameters) {
		this.parameters = parameters;
	}
	
	@Override
	public List<String> getSchemes() {
		return schemes;
	}
	public void setSchemes(List<String> schemes) {
		this.schemes = schemes;
	}
	
	@Override
	public Boolean getDeprecated() {
		return deprecated;
	}
	public void setDeprecated(Boolean deprecated) {
		this.deprecated = deprecated;
	}
	
	@Override
	public List<SwaggerResponse> getResponses() {
		return responses;
	}
	public void setResponses(List<SwaggerResponse> responses) {
		this.responses = responses;
	}
	
	@Override
	public List<SwaggerSecuritySetting> getSecurity() {
		return security;
	}
	public void setSecurity(List<SwaggerSecuritySetting> security) {
		this.security = security;
	}
	
	@Override
	public Map<String, Object> getExtensions() {
		return extensions;
	}
	public void setExtensions(Map<String, Object> extensions) {
		this.extensions = extensions;
	}

	@Override
	public String toString() {
		return method + "[" + operationId + "]";
	}
}

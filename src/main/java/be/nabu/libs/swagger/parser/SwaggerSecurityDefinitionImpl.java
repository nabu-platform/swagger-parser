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

import java.util.Map;

import be.nabu.libs.swagger.api.SwaggerParameter.ParameterLocation;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition;

public class SwaggerSecurityDefinitionImpl implements SwaggerSecurityDefinition {

	private String name, description, fieldName, authorizationUrl, tokenUrl;
	private SecurityType type;
	private ParameterLocation location;
	private OAuth2Flow flow;
	private Map<String, String> scopes;
	
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
	public String getFieldName() {
		return fieldName;
	}
	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}
	
	@Override
	public String getAuthorizationUrl() {
		return authorizationUrl;
	}
	public void setAuthorizationUrl(String authorizationUrl) {
		this.authorizationUrl = authorizationUrl;
	}
	
	@Override
	public String getTokenUrl() {
		return tokenUrl;
	}
	public void setTokenUrl(String tokenUrl) {
		this.tokenUrl = tokenUrl;
	}
	
	@Override
	public SecurityType getType() {
		return type;
	}
	public void setType(SecurityType type) {
		this.type = type;
	}
	
	@Override
	public ParameterLocation getLocation() {
		return location;
	}
	public void setLocation(ParameterLocation location) {
		this.location = location;
	}
	
	@Override
	public OAuth2Flow getFlow() {
		return flow;
	}
	public void setFlow(OAuth2Flow flow) {
		this.flow = flow;
	}
	
	@Override
	public Map<String, String> getScopes() {
		return scopes;
	}
	public void setScopes(Map<String, String> scopes) {
		this.scopes = scopes;
	}

}

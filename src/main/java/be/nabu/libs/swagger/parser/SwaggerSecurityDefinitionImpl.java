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

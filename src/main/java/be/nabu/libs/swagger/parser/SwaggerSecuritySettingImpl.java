package be.nabu.libs.swagger.parser;

import java.util.List;

import be.nabu.libs.swagger.api.SwaggerSecuritySetting;

public class SwaggerSecuritySettingImpl implements SwaggerSecuritySetting {

	private String name;
	private List<String> scopes;
	
	@Override
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public List<String> getScopes() {
		return scopes;
	}
	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}
	
}

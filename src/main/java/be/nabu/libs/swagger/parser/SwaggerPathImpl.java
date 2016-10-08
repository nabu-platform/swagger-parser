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
	
	
}

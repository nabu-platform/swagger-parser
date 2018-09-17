package be.nabu.libs.swagger.parser;

import java.util.List;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerInfo;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition;
import be.nabu.libs.swagger.api.SwaggerSecuritySetting;
import be.nabu.libs.types.api.TypeRegistry;

public class SwaggerDefinitionImpl implements SwaggerDefinition, Artifact {

	private String version = "2.0";
	private String host, basePath;
	private List<String> schemes, consumes, produces;
	private TypeRegistry registry;
	private String id;
	private SwaggerInfo info;
	private List<SwaggerPath> paths;
	private Map<String, SwaggerParameter> parameters;
	private List<SwaggerSecurityDefinition> securityDefinitions;
	private List<SwaggerSecuritySetting> globalSecurity;
	
	public SwaggerDefinitionImpl(String id) {
		this.id = id;
	}

	@Override
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public String getBasePath() {
		return basePath;
	}
	public void setBasePath(String basePath) {
		this.basePath = basePath;
	}

	@Override
	public List<String> getSchemes() {
		return schemes;
	}
	public void setSchemes(List<String> schemes) {
		this.schemes = schemes;
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
	public TypeRegistry getRegistry() {
		return registry;
	}
	public void setRegistry(TypeRegistry registry) {
		this.registry = registry;
	}

	@Override
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public SwaggerInfo getInfo() {
		return info;
	}
	public void setInfo(SwaggerInfo info) {
		this.info = info;
	}

	@Override
	public List<SwaggerPath> getPaths() {
		return paths;
	}
	public void setPaths(List<SwaggerPath> paths) {
		this.paths = paths;
	}

	@Override
	public String getId() {
		return id;
	}

	public Map<String, SwaggerParameter> getParameters() {
		return parameters;
	}
	public void setParameters(Map<String, SwaggerParameter> parameters) {
		this.parameters = parameters;
	}

	@Override
	public List<SwaggerSecurityDefinition> getSecurityDefinitions() {
		return securityDefinitions;
	}
	public void setSecurityDefinitions(List<SwaggerSecurityDefinition> securityDefinitions) {
		this.securityDefinitions = securityDefinitions;
	}

	public List<SwaggerSecuritySetting> getGlobalSecurity() {
		return globalSecurity;
	}
	public void setGlobalSecurity(List<SwaggerSecuritySetting> globalSecurity) {
		this.globalSecurity = globalSecurity;
	}
	
}

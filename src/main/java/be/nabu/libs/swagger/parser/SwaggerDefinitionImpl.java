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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import be.nabu.libs.artifacts.api.Artifact;
import be.nabu.libs.swagger.api.SwaggerDefinition;
import be.nabu.libs.swagger.api.SwaggerInfo;
import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerPath;
import be.nabu.libs.swagger.api.SwaggerSecurityDefinition;
import be.nabu.libs.swagger.api.SwaggerSecuritySetting;
import be.nabu.libs.swagger.api.SwaggerTag;
import be.nabu.libs.types.api.TypeRegistry;
import be.nabu.libs.validator.api.ValidationMessage;

public class SwaggerDefinitionImpl implements SwaggerDefinition, Artifact {

	private String definitionType = "swagger";
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
	private List<ValidationMessage> messages;
	private List<SwaggerTag> tags = new ArrayList<SwaggerTag>();
	
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

	@Override
	public String getDefinitionType() {
		return definitionType;
	}
	public void setDefinitionType(String definitionType) {
		this.definitionType = definitionType;
	}

	public List<ValidationMessage> getValidationMessages() {
		return messages;
	}
	public void setValidationMessages(List<ValidationMessage> messages) {
		this.messages = messages;
	}

	@Override
	public List<SwaggerTag> getTags() {
		return tags;
	}
	public void setTags(List<SwaggerTag> tags) {
		this.tags = tags;
	}
	
}

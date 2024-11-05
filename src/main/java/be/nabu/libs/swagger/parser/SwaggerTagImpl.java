package be.nabu.libs.swagger.parser;

import be.nabu.libs.swagger.api.SwaggerTag;

public class SwaggerTagImpl implements SwaggerTag {

	private String name, description;
	
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

}

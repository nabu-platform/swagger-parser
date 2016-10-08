package be.nabu.libs.swagger.parser;

import be.nabu.libs.swagger.api.SwaggerDocumentation;
import be.nabu.libs.types.api.ComplexContent;

public class SwaggerDocumentationImpl implements SwaggerDocumentation {

	public static SwaggerDocumentation parse(ComplexContent content) {
		SwaggerDocumentationImpl impl = new SwaggerDocumentationImpl();
		impl.setDescription((String) content.get("description"));
		impl.setUrl((String) content.get("url"));
		return impl;
	}
	
	private String description, url;

	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

}

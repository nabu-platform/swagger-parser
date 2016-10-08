package be.nabu.libs.swagger.parser;

import be.nabu.libs.swagger.api.SwaggerContact;
import be.nabu.libs.types.api.ComplexContent;

public class SwaggerContactImpl implements SwaggerContact {
	
	public static SwaggerContact parse(ComplexContent content) {
		SwaggerContactImpl contact = new SwaggerContactImpl();
		contact.setEmail((String) content.get("email"));
		contact.setUrl((String) content.get("url"));
		contact.setName((String) content.get("name"));
		return contact;
	}
	
	private String name, url, email;

	@Override
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
}

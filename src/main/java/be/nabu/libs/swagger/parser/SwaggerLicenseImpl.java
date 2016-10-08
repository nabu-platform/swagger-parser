package be.nabu.libs.swagger.parser;

import be.nabu.libs.swagger.api.SwaggerLicense;
import be.nabu.libs.types.api.ComplexContent;

public class SwaggerLicenseImpl implements SwaggerLicense {
	
	public static SwaggerLicense parse(ComplexContent content) {
		SwaggerLicenseImpl license = new SwaggerLicenseImpl();
		license.setUrl((String) content.get("url"));
		license.setName((String) content.get("name"));
		return license;
	}
	
	private String name, url;

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
	
}

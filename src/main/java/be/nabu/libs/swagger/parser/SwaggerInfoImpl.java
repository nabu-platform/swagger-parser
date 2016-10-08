package be.nabu.libs.swagger.parser;

import be.nabu.libs.swagger.api.SwaggerContact;
import be.nabu.libs.swagger.api.SwaggerInfo;
import be.nabu.libs.swagger.api.SwaggerLicense;
import be.nabu.libs.types.api.ComplexContent;

public class SwaggerInfoImpl implements SwaggerInfo {
	
	public static SwaggerInfo parse(ComplexContent content) {
		SwaggerInfoImpl info = new SwaggerInfoImpl();
		info.setTitle((String) content.get("title"));
		info.setDescription((String) content.get("description"));
		info.setVersion((String) content.get("version"));
		info.setTermsOfService((String) content.get("termsOfService"));
		if (content.get("contact") != null) {
			info.setContact(SwaggerContactImpl.parse((ComplexContent) content.get("contact")));
		}
		if (content.get("license") != null) {
			info.setLicense(SwaggerLicenseImpl.parse((ComplexContent) content.get("license")));
		}
		return info;
	}
	
	private String title, description, version, termsOfService;
	private SwaggerContact contact;
	private SwaggerLicense license;
	
	@Override
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	
	@Override
	public String getTermsOfService() {
		return termsOfService;
	}
	public void setTermsOfService(String termsOfService) {
		this.termsOfService = termsOfService;
	}
	
	@Override
	public SwaggerContact getContact() {
		return contact;
	}
	public void setContact(SwaggerContact contact) {
		this.contact = contact;
	}
	
	@Override
	public SwaggerLicense getLicense() {
		return license;
	}
	public void setLicense(SwaggerLicense license) {
		this.license = license;
	}
	
}

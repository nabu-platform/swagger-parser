package be.nabu.libs.swagger.parser;

import java.util.List;

import be.nabu.libs.swagger.api.SwaggerParameter;
import be.nabu.libs.swagger.api.SwaggerResponse;
import be.nabu.libs.types.api.Element;

public class SwaggerResponseImpl implements SwaggerResponse {

	private Integer code;
	private String description;
	private Element<?> element;
	private List<SwaggerParameter> headers;
	private List<String> produces;
	
	@Override
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	@Override
	public Element<?> getElement() {
		return element;
	}
	public void setElement(Element<?> element) {
		this.element = element;
	}
	
	@Override
	public List<SwaggerParameter> getHeaders() {
		return headers;
	}
	public void setHeaders(List<SwaggerParameter> headers) {
		this.headers = headers;
	}
	
	@Override
	public Integer getCode() {
		return code;
	}
	public void setCode(Integer code) {
		this.code = code;
	}
	
	@Override
	public List<String> getProduces() {
		return produces;
	}
	public void setProduces(List<String> produces) {
		this.produces = produces;
	}

	
}

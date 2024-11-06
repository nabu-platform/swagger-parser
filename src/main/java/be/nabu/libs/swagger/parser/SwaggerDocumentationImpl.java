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

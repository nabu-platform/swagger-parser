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

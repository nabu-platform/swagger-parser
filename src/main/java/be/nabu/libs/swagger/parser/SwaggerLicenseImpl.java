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

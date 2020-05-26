/*******************************************************************************
 * Copyright (C) 2013 Andrei Olaru, Marius-Tudor Benea, Nguyen Thi Thuy Nga, Amal El Fallah Seghrouchni, Cedric Herpson.
 * 
 * This file is part of tATAmI-PC.
 * 
 * tATAmI-PC is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * tATAmI-PC is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with tATAmI-PC.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.sclaim.constructs;

import java.io.Serializable;

/**
 * Structure used for any Claim construction. To be used only as a superclass for
 * any construct of the language.
 * 
 * @author tudor
 *
 */
public class ClaimConstruct implements Serializable{
	
	private static final long serialVersionUID = 2265048414600249685L;
	/**
	 * the type of the language construct
	 */
	private ClaimConstructType type;

	protected ClaimConstruct(ClaimConstructType type)
	{
		this.setType(type);
	}
	
	public void setType(ClaimConstructType type) {
		this.type = type;
	}
	public ClaimConstructType getType() {
		return type;
	}
}

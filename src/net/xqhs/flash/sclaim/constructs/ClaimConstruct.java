/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
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

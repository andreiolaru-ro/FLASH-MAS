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

/**
 * Structure used in order to keep the values of the variables. ClaimValue objects
 * are associated with variable names in symbol tables.
 * 
 * @author tudor
 *
 */
public class ClaimValue extends ClaimConstruct
{
	/**
	 * Value to output in toString if there is no value (avoids a {@link NullPointerException}).
	 */
	public static final String	NULL_VALUE_OUTPUT	= "<null>";
	
	private Object value;
	
	public ClaimValue()
	{
		super(ClaimConstructType.VALUE);
	}

	public ClaimValue(Object value)
	{
		super(ClaimConstructType.VALUE);
		setValue(value);
	}

	public void setValue(Object value) {
		this.value = value;
	}

	public Object getValue() {
		return value;
	}

	public String toString()
	{
		return (getValue() == null ? NULL_VALUE_OUTPUT : getValue().toString());
	}
	
	/**
	 * static method that parses a string and returns a ClaimValue with the name specified by the string.
	 * @param str - string to be parsed
	 */
	public static ClaimValue parseString(String str)
	{
		String newStr = str.trim();
		
		return new ClaimValue((Object) newStr);
	}
}

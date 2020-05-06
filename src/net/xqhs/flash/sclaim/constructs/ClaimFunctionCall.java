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

import java.util.Vector;

/**
 * Structure returned by the parser for a function call (either included in the language or not).
 * 
 * @author tudor
 *
 */
@SuppressWarnings("serial")
public class ClaimFunctionCall extends ClaimConstruct {
	private String functionName;
	
	/**
	 * Used to know whether the function is a Java one or a language one, case in which
	 * the field will indicate also the function itself.
	 */
	private ClaimFunctionType functionType;
	
	/**
	 * <em>arguments</em> will contain a vector of ClaimConstruct objects, with the following possible types:
	 * 1. VALUE
	 * 2. VARIABLE
	 * 3. STRUCTURE
	 * 4. FUNCTION_CALL
	 * 
	 */
	private Vector<ClaimConstruct> arguments;
	
	/**
	 * Constructor 
	 * @param type - the type of the function. May be LANGUAGE_FUNCTION_CALL for those functions which are included in the language, or FUNCTION_CALL, for any other function calls. 
	 * @param functionName - the name of the function
	 * @param behavior - the behavior to which this function call belongs
	 * @param arguments - the arguments corresponding to the function call
	 */
	public ClaimFunctionCall(ClaimFunctionType type, String functionName, Vector<ClaimConstruct> arguments)
	{
		super(ClaimConstructType.FUNCTION_CALL);
		
		setFunctionType(type);
		setFunctionName(functionName);
		setArguments(arguments);
	}

	public void setFunctionName(String functionName) {
		this.functionName = functionName;
	}

	public String getFunctionName() {
		return functionName;
	}

	public void setArguments(Vector<ClaimConstruct> arguments) {
		this.arguments = arguments;
	}

	public Vector<ClaimConstruct> getArguments() {
		return arguments;
	}

	public void setFunctionType(ClaimFunctionType functionType) {
		this.functionType = functionType;
	}

	public ClaimFunctionType getFunctionType() {
		return functionType;
	}

    @Override
    public String toString() {
        return "ClaimFunctionCall{" +
                "functionName='" + functionName + '\'' +
                ", functionType=" + functionType +
                ", arguments=" + arguments +
                '}';
    }
}

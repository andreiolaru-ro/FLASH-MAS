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
package examples.composite;

import java.util.Vector;

import tatami.core.agent.claim.ClaimFunctionLibrary;
import tatami.sclaim.constructs.basic.ClaimValue;


public class StringFunctions implements ClaimFunctionLibrary
{
	
	public static boolean equalString(Vector<ClaimValue> arguments)
	{
		String s1 = (String)arguments.get(0).getValue();
		String s2 = (String)arguments.get(1).getValue();
		return s1.equals(s2);
	}
	
}

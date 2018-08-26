/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package ChatAgents;

import java.util.Vector;

import net.xqhs.flash.sclaim.ClaimFunctionLibrary;
import net.xqhs.flash.sclaim.constructs.ClaimValue;


@SuppressWarnings("javadoc")
public class ChatFunctions implements ClaimFunctionLibrary
{
	public static boolean increment(Vector<ClaimValue> arguments)
	{
		arguments.set(1, new ClaimValue(new Integer(Integer.parseInt((String)arguments.get(0).getValue()) + 1).toString()));
		return true;
	}
	
	public static boolean initOutput(Vector<ClaimValue> arguments)
	{
		arguments.set(0, new ClaimValue(new String("")));
		return true;
	}
	
	public static boolean assembleOutput(Vector<ClaimValue> arguments)
	{
		String dir = (String)arguments.get(0).getValue();
		String seq = (String)arguments.get(1).getValue();
		String msg = (String)arguments.get(2).getValue();
		String output = (String)arguments.get(3).getValue();
		arguments.set(3, new ClaimValue(output + "\n(" + seq + ")" + dir + ": " + msg));
		return true;
	}
}

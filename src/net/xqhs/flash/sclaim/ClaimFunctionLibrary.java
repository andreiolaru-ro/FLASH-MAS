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
package net.xqhs.flash.sclaim;

import java.util.Vector;

import net.xqhs.flash.sclaim.constructs.ClaimValue;


/**
 * This interface does not specify any functions.
 * 
 * <p>
 * Rather, it is a tag for classes that act as a library of functions for CLAIM2 Agents.
 * 
 * <p>
 * Any functions in this library destined to be called in CLAIM code should be <code>public static</code> and should take one parameter: a {@link Vector} with a
 * number of components equal to the total number of arguments in the function; with the {@link ClaimValue} instances to which the arguments are bound (if they
 * are bound); and with null values for parameters that are not bound to values; the function may replace the null components of the vector with
 * {@link ClaimValue} instances that will make return values; each function should also return a boolean, to be used in conditional statements.
 * 
 * <p>
 * Example:
 * 
 * <p>
 * Take a function echo that in CLAIM will be called as follows: <br>
 * <code>(echo ?input ?output)</code><br>
 * 
 * This function would be declared in the library as<br/>
 * <code> public static boolean echo(Vector&lt;Object&gt; arguments)</code><br>
 * 
 * Assume that at runtime the function is called with ?input bound to a {@link String} "hello". This means that the function will be called with a Vector that
 * would print as [hello, null]. When the function returns (possibly returning <code>true</code>), the second component of the vector would also be a String
 * "hello".
 * 
 * <p>
 * Note: CLAIM will only bind those arguments of a function that were not bound before, regardless of the function changing any components of the vector.
 * 
 * @author Andrei Olaru
 * 
 */

public interface ClaimFunctionLibrary
{
	// function template:
	// 	public static boolean function(Vector<ClaimValue> arguments)	{}

}

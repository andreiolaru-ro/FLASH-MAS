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
 * enum that holds the possible types of Claim constructs
 * 
 * @author tudor
 *
 */
public enum ClaimConstructType
{
	 VARIABLE,
	 VALUE,
	 STRUCTURE,
	 FUNCTION_CALL,		// java function, or: receive send addK removeK readK in out move acid open new wait
	 IF,
	 CONDITION,
	 BEHAVIOR,
	 AGENT,
	 FORALLK,
	 WHILE,
	 AGOAL,
	 MGOAL,
	 PGOAL,
}

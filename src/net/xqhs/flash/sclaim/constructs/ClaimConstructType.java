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

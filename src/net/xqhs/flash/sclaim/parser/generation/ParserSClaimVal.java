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
//#############################################
//## file: ParserClaim2.java
//## Generated by Byacc/j
//#############################################
package net.xqhs.flash.sclaim.parser.generation;

import java.util.ArrayList;

/* ATTENTION: file used by the byacc/j parser generator. 
 * Any modification should be synchronized with the content of "parser.y",
 * then the parser should be generated again using "generateParser.sh"*/

import java.util.Vector;

import net.xqhs.flash.sclaim.constructs.ClaimBehaviorType;
import net.xqhs.flash.sclaim.constructs.ClaimConstruct;

/**
 * BYACC/J Semantic Value for parser: ParserClaim2
 * This class provides some of the functionality
 * of the yacc/C 'union' directive
 */
public class ParserSClaimVal
{
	/**
	 * integer value of this 'union'
	 */
	public int ival;

	/**
	 * double value of this 'union'
	 */
	public double dval;

	/**
	 * string value of this 'union'
	 */
	public String sval;

	/**
	 * object value of this 'union'
	 */
	public Object obj;

	//#############################################
	//## C O N S T R U C T O R S
	//#############################################
	/**
	 * Initialize me without a value
	 */
	public ParserSClaimVal()
	{
	}
	/**
	 * Initialize me as an int
	 */
	public ParserSClaimVal(int val)
	{
		ival=val;
	}

	/**
	 * Initialize me as a double
	 */
	public ParserSClaimVal(double val)
	{
		dval=val;
	}

	/**
	 * Initialize me as a string
	 */
	public ParserSClaimVal(String val)
	{
		sval=val;
	}

	/**
	 * Initialize me as an Object
	 */
	public ParserSClaimVal(Object val)
	{
		obj=val;
	}

	//#############################################
	//## USER DEFINED FIELDS AND CONSTRUCTORS
	//#############################################

	public ClaimConstruct claimConstruct;

	public ParserSClaimVal(ClaimConstruct claimConstruct) {
		super();
		this.claimConstruct = claimConstruct;
	}

	public Vector<ClaimConstruct> claimConstructVector;

	public ParserSClaimVal(Vector<ClaimConstruct> claimConstructVector) {
		super();
		this.claimConstructVector = claimConstructVector;
	}
	
	public ArrayList<String> stringVector;

	public ParserSClaimVal(ArrayList<String> stringVector) {
		super();
		this.stringVector = stringVector;
	}
	
	public ClaimBehaviorType claimBehaviorType;

	public ParserSClaimVal(ClaimBehaviorType claimBehaviorType) {
		super();
		this.claimBehaviorType = claimBehaviorType;
	}
}//end class

//#############################################
//## E N D    O F    F I L E
//#############################################

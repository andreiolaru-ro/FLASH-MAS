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
 * Structure returned by the parser for an if statement
 * 
 * @author tudor
 *
 */
public class ClaimIf extends ClaimConstruct
{
	/**
	 * May be a function call (either readK or a Java function) 
	 */
	private ClaimFunctionCall condition;
	/**
	 * May be a vector of any statement that could appear in a behavior 
	 */
	private Vector<ClaimConstruct> trueBranch;
	/**
	 * May be a vector of any statement that could appear in a behavior 
	 */
	private Vector<ClaimConstruct> falseBranch;
	
	public ClaimIf(ClaimFunctionCall condition, Vector<ClaimConstruct> trueBranch, Vector<ClaimConstruct> falseBranch)
	{
		super(ClaimConstructType.IF);
		setCondition(condition);
		setTrueBranch(trueBranch);
		setFalseBranch(falseBranch);
	}

	public void setCondition(ClaimFunctionCall condition) {
		this.condition = condition;
	}

	public ClaimFunctionCall getCondition() {
		return condition;
	}

	public void setTrueBranch(Vector<ClaimConstruct> trueBranch) {
		this.trueBranch = trueBranch;
	}

	public Vector<ClaimConstruct> getTrueBranch() {
		return trueBranch;
	}

	public void setFalseBranch(Vector<ClaimConstruct> falseBranch) {
		this.falseBranch = falseBranch;
	}

	public Vector<ClaimConstruct> getFalseBranch() {
		return falseBranch;
	}

}

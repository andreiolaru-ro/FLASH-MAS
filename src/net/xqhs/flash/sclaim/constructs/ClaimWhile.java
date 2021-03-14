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

import java.util.Vector;

/**
 * Structure returned by the parser for a while statement
 * 
 * @author tudor
 *
 */
public class ClaimWhile extends ClaimConstruct
{
	/**
	 * May be a function call, either readK or a Java function
	 */
	private ClaimFunctionCall condition;
	
	
	/**
	 * May be a vector of any statement that could appear in a behavior 
	 */
	private Vector<ClaimConstruct> statements;

	public ClaimWhile(ClaimFunctionCall condition, Vector<ClaimConstruct> statements)
	{
		super(ClaimConstructType.WHILE);
		setCondition(condition);
		setStatements(statements);
	}

	public void setStatements(Vector<ClaimConstruct> statements) {
		this.statements = statements;
	}

	public Vector<ClaimConstruct> getStatements() {
		return statements;
	}

	public ClaimFunctionCall getCondition() {
		return condition;
	}

	public void setCondition(ClaimFunctionCall condition) {
		this.condition = condition;
	}

}

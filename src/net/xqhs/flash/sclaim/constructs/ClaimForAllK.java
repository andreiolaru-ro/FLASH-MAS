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
 * Structure returned by the parser for a forAllK statement
 * 
 * @author tudor
 *
 */
public class ClaimForAllK extends ClaimConstruct
{
	private static final long serialVersionUID = -5092381799277193948L;

	/**
	 * A structure to be matched with the knowledge in the knowledge base. Must contain at least a variable
	 */
	private ClaimStructure structure;
	
	/**
	 * May be a vector of any statement that could appear in a behavior 
	 */
	private Vector<ClaimConstruct> statements;

	public ClaimForAllK(ClaimStructure structure, Vector<ClaimConstruct> statements)
	{
		super(ClaimConstructType.FORALLK);
		setStructure(structure);
		setStatements(statements);
	}

	public void setStructure(ClaimStructure structure) {
		this.structure = structure;
	}

	public ClaimStructure getStructure() {
		return structure;
	}

	public void setStatements(Vector<ClaimConstruct> statements) {
		this.statements = statements;
	}

	public Vector<ClaimConstruct> getStatements() {
		return statements;
	}

}

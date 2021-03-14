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

import java.io.Serializable;
import java.util.Vector;

/**
 * Structure returned by the parser for a behavior definition
 * 
 * @author tudor
 *
 */
public class ClaimBehaviorDefinition extends ClaimConstruct implements Serializable{
	private ClaimAgentDefinition myAgent;
	
	/**
	 * the name of the behavior
	 */
	private String name;
	private ClaimBehaviorType behaviorType;
	
	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	/**
	 * <em>statements</em> will contain a vector of ClaimConstruct objects, with the following possible types:
	 * 1. FUNCTION_CALL - included in the language or not
	 * 2. IF
	 * 3. CONDITION
	 * 
	 */
	private Vector<ClaimConstruct> statements;

	public ClaimBehaviorDefinition(String name, ClaimBehaviorType behaviorType, Vector<ClaimConstruct> statements)
	{
		super(ClaimConstructType.BEHAVIOR);
		setName(name);
		setBehaviorType(behaviorType);
		setStatements(statements);
	}

	public ClaimAgentDefinition getMyAgent() {
		return myAgent;
	}

	public void setMyAgent(ClaimAgentDefinition myAgent) {
		this.myAgent = myAgent;
	}

	public void setStatements(Vector<ClaimConstruct> statements) {
		this.statements = statements;
	}

	public Vector<ClaimConstruct> getStatements() {
		return statements; 
	}

	public void setBehaviorType(ClaimBehaviorType behaviorType) {
		this.behaviorType = behaviorType;
	}

	public ClaimBehaviorType getBehaviorType() {
		return behaviorType;
	}
}

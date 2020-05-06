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

import java.io.Serializable;
import java.util.List;
import java.util.Vector;

/**
 * Structure returned by the parser for an agent definition
 * 
 * @author tudor
 *
 */
public class ClaimAgentDefinition extends ClaimConstruct implements Serializable {

	private static final long	serialVersionUID	= 899020274466658119L;
	
	private Vector<ClaimVariable> parameters;
	private Vector<ClaimBehaviorDefinition> behaviors;
	private List<Class<?>> codeAttachements = new Vector<Class<?>>();
	
	/**
	 * the name of the agent class
	 */
	private String agentClassName;

	public void setClassName(String name) {
		this.agentClassName = name;
	}

	public String getClassName() {
		return agentClassName;
	}
	
	public ClaimAgentDefinition(String agentClassName, Vector<ClaimConstruct> parameters, Vector<ClaimBehaviorDefinition> behaviors)
	{
		
		super(ClaimConstructType.AGENT);
		setClassName(agentClassName);
		setParameters(parameters);
		setBehaviors(behaviors);
	}

/*	public ClaimAgentDefinition(String agentClassName, Vector<ClaimVariable> parameters, Vector<ClaimBehaviorDefinition> behaviors, List<Class<?>> codeAttachements)
	{
		
		super(ClaimConstructType.AGENT);
		setClassName(agentClassName);
		this.parameters = parameters;
		setBehaviors(behaviors);
		if(codeAttachements != null)
			this.codeAttachements = codeAttachements;
	}*/

	public void setParameters(Vector<ClaimConstruct> parameters) {
		this.parameters = new Vector<ClaimVariable>();
		if (parameters!=null)
			for (ClaimConstruct currentConstruct : parameters)
				this.parameters.add((ClaimVariable) currentConstruct);
	}

	public Vector<ClaimVariable> getParameters() {
		return parameters;
	}

	public void setBehaviors(Vector<ClaimBehaviorDefinition> behaviors) {
		this.behaviors = behaviors;
	}

	public Vector<ClaimBehaviorDefinition> getBehaviors() {
		return behaviors;
	}
	
	public ClaimAgentDefinition addCodeAttachement(Class<?> clazz)
	{
		codeAttachements.add(clazz);
		return this;
	}
	
	public List<Class<?>> getCodeAttachments()
	{
		return new Vector<Class<?>>(codeAttachements);
	}
	
/*	public ClaimAgentDefinition createCopy() {
		ClaimAgentDefinition copy = new ClaimAgentDefinition(getClassName(), getParameters(), getBehaviors(), null);
		
		return copy;
	}*/

    @Override
    public String toString() {
        return "ClaimAgentDefinition{" +
                "parameters=" + parameters +
                ", behaviors=" + behaviors +
                ", codeAttachements=" + codeAttachements +
                ", agentClassName='" + agentClassName + '\'' +
                '}';
    }
}

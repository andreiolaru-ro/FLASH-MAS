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
 * Structure returned by the parser for a condition statement
 * 
 * @author tudor
 *
 */
public class ClaimCondition extends ClaimConstruct
{
	/**
	 * May be a function call, either readK or a Java function
	 */
	private ClaimFunctionCall condition;
	
	public ClaimCondition(ClaimFunctionCall condition)
	{
		super(ClaimConstructType.CONDITION);
		setCondition(condition);
	}

	public void setCondition(ClaimFunctionCall condition) {
		this.condition = condition;
	}

	public ClaimFunctionCall getCondition() {
		return condition;
	}

    @Override
    public String toString() {
        return "ClaimCondition{" +
                "condition=" + condition +
                '}';
    }
}

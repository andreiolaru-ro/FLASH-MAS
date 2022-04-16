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
package net.xqhs.flash.ent_op.model;

import java.util.ArrayList;
import java.util.Set;

/**
 * An instance of this class describes an operation that can be performed on an entity, the arguments for the operation,
 * and the conditions under which the operation can be accessed by a caller entity.
 * 
 * @author Andrei Olaru
 */
public interface Operation {
	interface Description {
	}
	
	interface Restriction {
	}
	
	interface Value {
		String getType();
		
		Description getDescription();
	}
	
	String getName();

	String getOwner();
	
	Description getDescription();
	
	boolean hasResult();
	
	Value getResultType();
	
	ArrayList<Value> getArguments();
	
	Set<Restriction> getRestrictions();
}

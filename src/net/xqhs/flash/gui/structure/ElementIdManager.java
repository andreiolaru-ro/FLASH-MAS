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
package net.xqhs.flash.gui.structure;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ElementIdManager {
	protected Map<String, Integer> idCounter = new HashMap<>();
	
	protected Map<String, Element>	idToElement	= new HashMap<>();
	protected Map<String, String>	idToEntity	= new HashMap<>();

	/**
	 * Create the ID for an element using the given parameters.
	 *
	 * @param entity
	 *  		- the entity for which the ID is created. Can be null.
	 * @param port
	 *  		- the port of the entity for which the ID is created. Cannot be null.
	 * @param role
	 * 		- the role of the entity for which the ID is created. Cannot be null.
	 *
	 * @return
	 * 		- A string of the ID.
	 */
	public String makeID(String entity, String port, String role) {
		return (entity != null ? entity + "_" : "") + port + "_" + role + "_";
	}

	/**
	 * Create the ID for an element using the given parameters.
	 *
	 * @param entity
	 *  		- the entity for which the ID is created. Can be null.
	 * @param port
	 * 		- the port of the entity for which the ID is created. Cannot be null.
	 * 
	 * @return
	 * 		- A string of the ID.
	 */
	public String makeID(String entity, String port) {
		return makeID(null, entity, port);
	}

	/**
	 * Retrieves a list of IDs that match the given entity, port, and role combination.
	 *
	 * @param entity
	 * 		- the entity name
	 * @param port
	 * 		- the port name
	 * @param role
	 * 		- the role name
	 *
	 * @return a list of IDs that match the given combination of entity, port, and role
	 */
	public List<String> getIDs(String entity, String port, String role) {
		String prefix = makeID(entity, port, role);
		List<String> result = new LinkedList<>();
		if(!idCounter.containsKey(prefix))
			return result;
		for(int i = 0; i <= idCounter.get(prefix).intValue(); i++)
			result.add(prefix + i);
		return result;
	}
	
	protected String newID(String entity, String port, String role) {
		String result = makeID(entity, port, role);
		if(idCounter.containsKey(result)) {
			int count = idCounter.get(result);
			idCounter.put(result, ++count);
			result += count;
		}
		else {
			idCounter.put(result, 0);
			result += 0;
		}
		return result;
	}
	
	protected void insertIdInto(Element element, String entity) {
		element.setId(newID(entity, element.getPort(), element.getRole()));
		idToElement.put(element.getId(), element);
		if(entity != null)
			idToEntity.put(element.getId(), entity);
	}

	/**
	 * Insert IDs into the given element and all its children.
	 *
	 * @param element
	 * 		- the element into which to insert the IDs.
	 *
	 * @return
	 * 		- the element with the IDs inserted.
	 */
	public Element insertIdsInto(Element element) {
		insertIdInto(element, null);
		if(element.getChildren() != null && !element.getChildren().isEmpty()) {
			for(int i = 0; i < element.getChildren().size(); i++) {
				element.getChildren().set(i, insertIdsInto(element.getChildren().get(i)));
			}
		}
		return element;
	}

	/**
	 * Insert IDs into the given element and all its children.
	 *
	 * @param element
	 * 		- the element into which to insert the IDs.
	 * @param entity
	 * 		- the entity for which the IDs are inserted.
	 *
	 * @return
	 * 		- the element with the IDs inserted.
	 */
	public Element insertIdsInto(Element element, String entity) {
		insertIdInto(element, entity);
		if(element.getChildren() != null && !element.getChildren().isEmpty()) {
			for(int i = 0; i < element.getChildren().size(); i++) {
				element.getChildren().set(i, insertIdsInto(element.getChildren().get(i), entity));
			}
		}
		return element;
	}

	/**
	 * Remove all IDs that start with the given prefix.
	 *
	 * @param prefix
	 * 		- the prefix of the IDs to remove.
	 */
	public void removeIdsWithPrefix(String prefix) {
		Set<String> toRemove = new HashSet<>();
		for(String key : idCounter.keySet())
			if(key.startsWith(prefix))
				toRemove.add(key);
		for(String key : toRemove) {
			idCounter.remove(key);
			idToElement.remove(key);
		}
	}

	/**
	 * Get the element with the given ID.
	 *
	 * @param id
	 *    - the ID of the element.
	 *
	 * @return
	 * 	  - the element with the given ID.
	 */
	public Element getElement(String id) {
		return idToElement.get(id);
	}

/**
	 * Get the entity with the given ID.
	 *
	 * @param id
	 *    - the ID of the entity.
	 *
	 * @return
	 * 	  - a string of the entity with the given ID.
	 */

	public String getEntity(String id) {
		return idToEntity.get(id);
	}
}

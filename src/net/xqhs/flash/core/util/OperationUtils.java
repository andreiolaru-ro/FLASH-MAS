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
package net.xqhs.flash.core.util;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class OperationUtils {

	/**
	 * Possible control operations.
	 */
	public static enum ControlOperation {
		/**
		 * Operation for starting an agent.
		 */
		START,
		/**
		 * Operation for stopping an agent.
		 */
		STOP,
		/**
		 * Operation where the Node stop the agent.
		 */
		KILL,
		/**
		 * Operation for pausing the simulation.
		 */
		PAUSE_SIMULATION,
		/**
		 * Operation for starting simulation.
		 */
		START_SIMULATION,
		/**
		 * Operation for stopping the simulation.
		 */
		STOP_SIMULATION,

		;

		/**
		 * Get the operation name.
		 *
		 * @return - operation name.
		 */
		public String getOperation() {
			return name().toLowerCase();
		}

		/**
		 * Return the operation matching the operation name given as parameter. If no operation matches the name, null
		 *
		 * @param name
		 *            - operation name.
		 *
		 * @return - operation matching the name.
		 */
		public static ControlOperation fromOperation(String name) {
			try {
				return valueOf(name.toUpperCase());
			} catch(IllegalArgumentException e) {
				return null;
			}
		}
	}

	/**
	 * Possible operations when performing monitoring.
	 */
	public static enum MonitoringOperation {
		/**
		 * Operation for updating the status of an entity.
		 */
		STATUS_UPDATE,
		/**
		 * Operation for updating the GUI of an entity.
		 */
		GUI_UPDATE,
		
		GUI_OUTPUT,
		
		GUI_INPUT_TO_ENTITY,
		
		;

		/**
		 * Get the operation name.
		 *
		 * @return - operation name.
		 */
		public String getOperation() {
			return name().toLowerCase();
		}

		/**
		 * Return the operation matching the operation name given as parameter. If no operation matches the name, null
		 *
		 * @param name
		 * 			   - operation name.
		 *
		 * @return - operation matching the name.
		 */
		public static MonitoringOperation fromOperation(String name) {
			try {
				return valueOf(name.toUpperCase());
			} catch(IllegalArgumentException e) {
				return null;
			}
		}
	}
	
	/**
	 * Possible access when performing an operation. `proxy` refers to an intermediate entity which is able to perform
	 * the operation; `self` refers to the ability to perform the operation by itself;
	 */
	private static final String[] modelAccess = { "proxy", "self" };
	
	/**
	 * Name of the operation.
	 */
	public static final String NAME = "name";
	
	/**
	 * Parameters of operation.
	 */
	public static final String PARAMETERS = "params";
	
	/**
	 * Value of operation in case it has one.
	 */
	public static final String VALUE = "value";
	
	/**
	 * Proxy for operation performing.
	 */
	public static final String PROXY = "proxy";
	
	/**
	 * Access mode for operation.
	 */
	public static final String ACCESS_MODE = "access";
	
	/**
	 * Parent node for entity registration.
	 */
	public static final String NODE = "node";
	
	/**
	 * Category of entity. e.g. support, agent, node etc.
	 */
	public static final String CATEGORY = "category";
	
	/**
	 * Operations available for an entity.
	 */
	public static final String OPERATIONS = "operations";
	
	/**
	 * Create a JSON object which represents an operation that certain entity is able to perform.
	 *
	 * @param name
	 *            - name of operation
	 * @param proxy
	 *            - proxy entity as a way to perform the operation
	 * @param value
	 *            - value for given operation
	 * @param param
	 *            - parameter as entity on which the operation is performed
	 * @return - json object encapsulating all operation details
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject operationToJSON(String name, String proxy, String value, String param) {
		JSONObject op = new JSONObject();
		op.put(NAME, name);
		op.put(PARAMETERS, param);
		op.put(VALUE, value);
		op.put(PROXY, proxy);
		if(name.equals("start"))
			op.put(ACCESS_MODE, modelAccess[0]);
		else
			op.put(ACCESS_MODE, modelAccess[1]);
		return op;
	}
	
	/**
	 * Create a JSON object which encapsulates necessary details to register an entity.
	 *
	 * @param node
	 *            - the node in the context of which is located
	 * @param category
	 *            - category of entity. e.g. support, agent, node etc
	 * @param name
	 *            - the name of the entity
	 * @param operations
	 *            - all operations this entity is able to perform
	 * @return - json object encapsulating all details
	 */
	@SuppressWarnings("unchecked")
	public static JSONObject registrationToJSON(String node, String category, String name, JSONArray operations) {
		JSONObject entity = new JSONObject();
		entity.put(NODE, node);
		entity.put(CATEGORY, category);
		entity.put(NAME, name);
		entity.put(OPERATIONS, operations);
		return entity;
	}
}

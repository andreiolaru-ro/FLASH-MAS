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
package net.xqhs.flash.core.monitoring;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.control.ControlShard;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.OperationUtils.ControlOperation;
import net.xqhs.flash.core.util.OperationUtils.MonitoringOperation;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.gui.GUILoad;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.util.logging.Unit;
import web.WebEntity;

/**
 * This class is used to monitor and control the MAS.
 */
public class CentralMonitoringAndControlEntity extends Unit implements Entity<Pylon> {

	protected class EntityData {
		String		name;
		String		status;
		JsonArray	operations;
		Element		guiSpecification;

		public String getName() {
			return name;
		}

		public String getStatus() {
			return status;
		}

		public JsonArray getOperations() {
			return operations;
		}

		public Element getGuiSpecification() {
			return guiSpecification;
		}

		public EntityData setName(String name) {
			this.name = name;
			return this;
		}

		public EntityData setStatus(String status) {
			this.status = status;
			return this;
		}

		@SuppressWarnings("unchecked")
		public EntityData addOperations(JsonArray ops) {
			if(operations == null)
				operations = new JsonArray();
			operations.addAll(ops);
			return this;
		}

		public EntityData setGuiSpecification(Element guiSpecification) {
			this.guiSpecification = guiSpecification;
			return this;
		}

		public EntityData insertNewGuiElements(List<Element> elements) {
			int i = 0;
			List<Element> interfaceElements = guiSpecification.getChildren();
			for(Element e : elements) {
				boolean found = false;
				for(Element ie : interfaceElements)
					if(e.getPort().equals(ie.getPort()))
						found = true;
				if(!found)
					interfaceElements.add(i++, e);
			}
			return this;
		}
	}

    /**
     * The central entity is a singleton.
     */
	public class CentralEntityProxy implements ShardContainer {
		@Override
		public AgentShard getAgentShard(AgentShardDesignation designation) {
			return null;
		}

		@Override
		public String getEntityName() {
			return getName();
		}

		/**
		 * This is expected to be called by the messaging shard.
		 */
		@Override
		public void postAgentEvent(AgentEvent event) {
			switch(event.getType()) {
			case AGENT_WAVE:
				parseReceivedMsg((AgentWave) event);
				break;
			default:
				break;
			}
		}
	}

	{
		setUnitName("M&C");
		setLoggerType(PlatformUtils.platformLogType());
	}

	/**
	 * Use this in conjunction with {@link DeploymentConfiguration#CENTRAL_NODE_KEY} to switch on the web interface.
	 */
	public static final String		WEB_INTERFACE_SWITCH	= "web";
	/**
	 * Use this in conjunction with {@link DeploymentConfiguration#CENTRAL_NODE_KEY} to switch on the swing interface.
	 */
	public static final String		SWING_INTERFACE_SWITCH	= "swing";
	/**
	 * Endpoint element for this shard.
	 */
	protected static final String	SHARD_ENDPOINT			= ControlShard.SHARD_ENDPOINT;

	/**
	 * Endpoint element for shards of control.
	 */
	protected static final String OTHER_CONTROL_SHARD_ENDPOINT = ControlShard.SHARD_ENDPOINT;

	/**
	 * Prefix to names of ports inserted by the central entity.
	 */
	protected static final String CONTROL_OPERATIONS_PREFIX = "control-";

    /**
     * Allow to get the control operation prefix from outside.
     *
     * @return the control operation prefix
     */
	public static String getControlOperationsPrefix() {
		return CONTROL_OPERATIONS_PREFIX;
	}

	/**
	 * Used for operations sent to the Node.
	 */
	protected static final String NODE_OPERATIONS_PREFIX = "nodeCtrl-";

    /**
     * Allow to get the node operation prefix from outside.
     *
     * @return the node operation prefix
     */
	public static String getNodeOperationsPrefix() {
		return NODE_OPERATIONS_PREFIX;
	}

	/**
	 * Used for the standard operation of starting entities.
	 */
	protected static final String STANDARD_OPERATON_START = "start";

	/**
	 * Used for the standard operation of stopping entities.
	 */
	protected static final String STANDARD_OPERATON_STOP = "stop";

	/**
	 * Used for unknown entity names, statuses, etc.
	 */
	public static final String UNKNOWN = "none";
	/**
	 * File for configuring the default controls for entities.
	 */
	protected static final String	DEFAULT_CONTROLS	= "controls.yml";

	private MessagingShard centralMessagingShard;

	private String name;

	private CentralGUI gui;

	private static boolean isRunning;

	/**
	 * Keeps track of all agents deployed in the system and their {@link List} of operations.
	 */
	private HashMap<String, List<String>> allAgents = new LinkedHashMap<>();

	/**
	 * Keeps track of all entities deployed in the system and their {@link JsonArray} of operations.
	 */
	private HashMap<String, JsonArray> entitiesToOp = new LinkedHashMap<>();

	/**
	 * Keeps track of entities state.
	 */
	private HashMap<String, String> entitiesState = new LinkedHashMap<>();

	protected Map<String, EntityData> entitiesData = new HashMap<>();

	protected Element standardCtrls;

	/**
	 * Keeps track of all nodes deployed in the system, along with their {@link List} of entities, indexed by their
	 * categories and names.
	 */
	private HashMap<String, HashMap<String, List<String>>> allNodeEntities = new LinkedHashMap<>();

	public ShardContainer proxy;

    /**
     * Creates a new instance of this class.
     *
     * @param configuration
     *            the configuration for this entity.
     */
	public CentralMonitoringAndControlEntity(MultiTreeMap configuration) {
		this.name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		proxy = new CentralEntityProxy();
		standardCtrls = GUILoad.load(new MultiTreeMap().addOneValue(GUILoad.FILE_SOURCE_PARAMETER, DEFAULT_CONTROLS)
				.addOneValue(CategoryName.PACKAGE.s(), this.getClass().getPackage().getName()), getLogger());

		for(String iface : configuration.getValues(DeploymentConfiguration.CENTRAL_NODE_KEY))
			switch(iface) {
			case WEB_INTERFACE_SWITCH: {
				// TODO mock config -- to be added in deployment configuration?
				MultiTreeMap config = new MultiTreeMap().addOneValue("file", "interface-files/model-page/web-page.yml");
				gui = new WebEntity();
				gui.addContext(proxy);
				if(gui.start()) // starts now in order to be available before starting entities
					li("web gui started");
				break;
			}
			case SWING_INTERFACE_SWITCH: {
				// TODO Swing GUI
				// gui = new GUIBoard(new CentralEntityProxy());
				// SwingUtilities.invokeLater(() -> {
				// try {
				// gui.setVisible(true);
				// } catch (RuntimeException e) {
				// e.printStackTrace();
				// }
				// });
				break;
			}
			}

	}

	/**
     * Parses the received message and calls the appropriate method.
     *
	 * @param wave
     *          - the {@link AgentWave} to be parsed
     *
	 * @return - an indication of success
	 */
	public boolean parseReceivedMsg(AgentWave wave) {
		String source = wave.getFirstSource();
		String content = wave.getContent();
		if(MonitoringOperation.GUI_INPUT_TO_ENTITY.getOperation().equals(wave.getFirstDestinationElement())) {
			// the first destination is removed
			// destinations is changed from: gui_input_to_entity / entity / gui / port
			// to: entity / monitoring / gui_input_to_entity / gui / port
			wave.removeFirstDestinationElement();
			String entity = wave.popDestinationElement();
			String port = wave.getValues(AgentWave.DESTINATION_ELEMENT)
					.get(wave.getValues(AgentWave.DESTINATION_ELEMENT).size() - 1);
			if (port.startsWith(NODE_OPERATIONS_PREFIX))
				entity = getParentNode(entity);

			if (port.startsWith(NODE_OPERATIONS_PREFIX) || port.startsWith(CONTROL_OPERATIONS_PREFIX)) {
				ControlOperation op = ControlOperation
						.fromOperation(port.split("-")[1]);
				AgentWave ctrlWave = new AgentWave(content, entity, ControlShard.SHARD_ENDPOINT, op.getOperation())
						.addSourceElements(SHARD_ENDPOINT);
				centralMessagingShard.sendMessage(ctrlWave.getCompleteSource(), ctrlWave.getCompleteDestination(),
						ctrlWave.getSerializedContent());

			} else { // normal input
				wave.removeFirstDestinationElement().recomputeCompleteDestination()
						.prependDestination(MonitoringOperation.GUI_INPUT_TO_ENTITY.getOperation())
						.prependDestination(MonitoringShard.SHARD_ENDPOINT).prependDestination(entity);
				wave.addSourceElementFirst(getName());
			}
			li("wave: ", wave);
			centralMessagingShard.sendMessage(wave.getCompleteSource(), wave.getCompleteDestination(),
					wave.getSerializedContent());
			return true;
		}
		JsonElement obj = null;
		try {
			obj = JsonParser.parseString(content);
			if(obj == null)
				return false;
		} catch(JsonSyntaxException e) {
			le("null/unparsable message content from []: ", source, content);
			return false;
		}
		if(obj.isJsonObject()) {
			JsonObject jo = obj.getAsJsonObject();
			if(manageOperation(jo)) {
				lf("Parsed operation from []: ", source, jo);
				return true;
			}
			else if(jo.get(OperationUtils.OPERATION_NAME).getAsString().equals("message")) {
				// WebEntity.agentMessages.put(source, ((String) jo.get("value")));
				lf("Parsed message from []: ", source, jo);
				return true;
			}
		}
		if(obj.isJsonArray()) {
			JsonArray ja = obj.getAsJsonArray();
			if(registerEntities(ja)) {
				lf("Registered entities from []: ", source, ja);
				return true;
			}
		}
		le("unknown message content from []: ", source, obj);
		return false;
	}

	/**
	 * This analysis the operation received and performs it.
	 *
	 * @param jsonObj
	 *            - the object received as content through the {@link ShardContainer}
	 * @return - an indication of success
	 */
	private boolean manageOperation(JsonObject jsonObj) {
		switch(MonitoringOperation.fromOperation(jsonObj.get(OperationUtils.OPERATION_NAME).getAsString())) {
			case STATUS_UPDATE:
				li("Status update received: " + jsonObj);
				String params = jsonObj.get(OperationUtils.PARAMETERS).getAsString();
				String value = jsonObj.get(OperationUtils.VALUE).getAsString();
				try {
					switch (AgentEvent.AgentEventType.valueOf(value)) {
						case AGENT_START:
							//the first time the agent is started, it's entitiesData is empty
							if (entitiesData.size() == 0){
								standardCtrls.getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_START)
										.get(0).setRole(Element.DISABLED_ROLE_PREFIX + Element.ACTIVE_INPUT);
								standardCtrls.getChildren(CONTROL_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP)
										.get(0).setRole(Element.ACTIVE_INPUT);
								standardCtrls.getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP)
										.get(0).setRole(Element.ACTIVE_INPUT);
							}
							else {
								entitiesData.get(params).getGuiSpecification().getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_START)
										.get(0).setRole(Element.DISABLED_ROLE_PREFIX + Element.ACTIVE_INPUT);
								entitiesData.get(params).getGuiSpecification().getChildren(CONTROL_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP)
										.get(0).setRole(Element.ACTIVE_INPUT);
								entitiesData.get(params).getGuiSpecification().getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP)
										.get(0).setRole(Element.ACTIVE_INPUT);
							}
							break;

						case AGENT_STOP:
							entitiesData.get(params).getGuiSpecification().getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_START)
									.get(0).setRole(Element.ACTIVE_INPUT);
							entitiesData.get(params).getGuiSpecification().getChildren(CONTROL_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP)
									.get(0).setRole(Element.DISABLED_ROLE_PREFIX + Element.ACTIVE_INPUT);
							entitiesData.get(params).getGuiSpecification().getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP)
									.get(0).setRole(Element.DISABLED_ROLE_PREFIX + Element.ACTIVE_INPUT);

							break;

						default:
							break;
					}

					li("Agent [] start button is now: ", params, entitiesData.get(params).getGuiSpecification()
							.getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_START).get(0).getRole());
					li("Agent [] stop button is now: ", params, entitiesData.get(params).getGuiSpecification()
							.getChildren(CONTROL_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP).get(0).getRole());
					li("Agent [] kill button is now: ", params, entitiesData.get(params).getGuiSpecification()
							.getChildren(NODE_OPERATIONS_PREFIX + STANDARD_OPERATON_STOP).get(0).getRole());

				} catch (Exception e) {
					le("Entity [] may not be an agent, or hasn't entitiesData yet. ", params);
				}
				entitiesState.put(params, value);
				lf("Entity [] status is now [].", params, value);
				// entitiesData.get(params).setStatus(value);
				// TODO
				// SwingUtilities.invokeLater(() -> {
				// try {
				// gui.updateStateOfEntity(params, value);
				// } catch (RuntimeException e) {
				// e.printStackTrace();
				// }
				// });
				return true;

			case GUI_UPDATE:
				li("GUI update received: " + jsonObj);
				String entity = jsonObj.get(OperationUtils.PARAMETERS).getAsString();
				if(!entitiesData.containsKey(entity)) {
					le("Entity [] is unknown.", entity);
					return false;
				}
				Element interfaceContainer = new Element();
				Element interfaceStructure = GUILoad.fromYaml(jsonObj.get(OperationUtils.VALUE).getAsString());
				if (interfaceStructure != null){
					// must avoid adding it twice
					for (Element child : interfaceStructure.getChildren()) {
						if (!interfaceContainer.getChildren().contains(child))
							interfaceContainer.addChild(child);
					}
				}
				try {
					interfaceContainer.addAllChildren(((Element) standardCtrls.clone()).getChildren());
				} catch (CloneNotSupportedException e) {
					throw new RuntimeException(e);
				}
				entitiesData.get(entity).setGuiSpecification(interfaceContainer);
				lf("Interface to [] reset to:", entity, interfaceContainer);
				return gui.updateGui(entity, interfaceContainer);

			case GUI_OUTPUT:
				String output_entity = jsonObj.get(OperationUtils.PARAMETERS).getAsString();
				AgentWave wave;
				try {
					wave = (AgentWave) MultiValueMap
							.fromSerializedString(jsonObj.get(OperationUtils.VALUE).getAsString());
				} catch(ClassNotFoundException e) {
					le("Unable to unpack AgentWave from ", output_entity);
					return false;
				}
				lf("The wave: ", wave);
				gui.sendOutput(wave);
				return true;

			default :
				le("Unknown operation: ", jsonObj.get(OperationUtils.OPERATION_NAME));
				return false;
		}
	}
	
	private boolean registerEntities(JsonArray ja) {
		for(Object o : ja) {
			JsonObject entityJson = (JsonObject) o;
			
			String node = entityJson.get(OperationUtils.NODE).getAsString();
			String category = entityJson.get(OperationUtils.CATEGORY).getAsString();
			String entity = entityJson.get(OperationUtils.OPERATION_NAME).getAsString();
			
			JsonArray operationDetails = (JsonArray) entityJson.get(OperationUtils.OPERATIONS);
			if(category.equals("agent")) {
				if(!allAgents.containsKey(entity))
					allAgents.put(entity, new LinkedList<>());
				for(Object oo : operationDetails) {
					JsonObject op = (JsonObject) oo;
					String operation = op.get(OperationUtils.OPERATION_NAME).getAsString();
					allAgents.get(entity).add(operation);
				}
			}
			entitiesToOp.put(entity, operationDetails);
			
			if(!allNodeEntities.containsKey(node))
				allNodeEntities.put(node, new LinkedHashMap<>());
			if(!allNodeEntities.get(node).containsKey(category))
				allNodeEntities.get(node).put(category, new LinkedList<>());
			allNodeEntities.get(node).get(category).add(entity);
			
			try {
				entitiesData.put(entity, new EntityData().setName(entity).setStatus(UNKNOWN).addOperations(operationDetails)
						.setGuiSpecification((Element) standardCtrls.clone()));
			} catch(CloneNotSupportedException e) {
				le("Failed to clone standard controls", e);
			}
			li("Registered entity []/[] in []", entity, category, node);
			gui.updateGui(entity, entitiesData.get(entity).getGuiSpecification());
		}
		return true;
	}
	
	/**
	 * @param childEntity
	 *            - an ordinary entity.
	 * @return - the name of its parent in context hierarchy - as a node - .
	 */
	public String getParentNode(String childEntity) {
		for(Map.Entry<String, HashMap<String, List<String>>> entry : allNodeEntities.entrySet()) {
			for(Map.Entry<String, List<String>> stringListEntry : entry.getValue().entrySet()) {
				if(stringListEntry.getValue().contains(childEntity))
					return entry.getKey();
			}
		}
		return null;
	}
	
	/**
	 * @return - a {@link List} of all nodes deployed in the system by their names
	 */
	public List<String> getNodes() {
		List<String> nodes = new LinkedList<>();
		allNodeEntities.entrySet().forEach(entry -> {
			nodes.add(entry.getKey());
		});
		return nodes;
	}
	
	/**
	 *
	 * @param operation
	 *            - command to be sent to all agents registered
	 */
	public void sendToAllAgents(String operation) {
		allAgents.entrySet().forEach(entry -> {
			if(!sendToEntity(entry.getKey(), operation))
				return;
		});
	}
	
	/**
	 * Sends a control message to a specific entity which will further perform the operation.
	 * 
	 * @param destination
	 *            - the name of destination entity
	 * @param operation
	 *            - operation to be performed on this entity
	 * @return - an indication of success
	 */
	public boolean sendToEntity(String destination, String operation) {
		JsonObject jsonOperation = getOperationFromEntity(destination, operation);
		if(jsonOperation == null) {
			le("Entity [] does not exist or does not support [] command.", destination, operation);
			return false;
		}
		String access = jsonOperation.get(OperationUtils.ACCESS_MODE).getAsString();
		if(access.equals(OperationUtils.ACCESS_MODE_SELF)) {
			JsonObject msg = OperationUtils.operationToJSON(operation, destination, "", destination);
			if(!sendMessage(destination, msg.toString())) {
				le("Message from [] to [] failed.", getName(), destination);
				return false;
			}
		}
		else {
			String op_proxy = jsonOperation.get(OperationUtils.PROXY).getAsString();
			JsonObject msg = OperationUtils.operationToJSON(operation, op_proxy, "", destination);
			if(!sendMessage(op_proxy, msg.toString())) {
				le("Message from [] to proxy [] of [] failed.", getName(), op_proxy, destination);
				return false;
			}
		}
		return true;
	}

    /**
	 * Returns the {@link JsonObject} corresponding to the operation of the given entity.
	 *
	 * @param entity
	 *            - the name of the entity
	 * @param command
	 *            - the name of the command
	 * @return - the {@link JsonObject} of the operation
	 */
	private JsonObject getOperationFromEntity(String entity, String command) {
		JsonArray ja = entitiesToOp.get(entity);
		if(ja == null)
			return null;
		for(Object o : ja) {
			JsonObject op = (JsonObject) o;
			String cmd = op.get(OperationUtils.OPERATION_NAME).getAsString();
			if(cmd.equals(command))
				return op;
		}
		return null;
	}
	
	/**
	 * @param destination
	 *            - the name of the destination entity
	 * @param content
	 *            - the content to be sent
	 * @return - an indication of success
	 */
	private boolean sendMessage(String destination, String content) {
		return centralMessagingShard.sendMessage(AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(destination, OTHER_CONTROL_SHARD_ENDPOINT), content);
	}

    /**
     * Sends a message to a specific agent
     *
     * @param agent
     *        - the name of the agent
     * @param content
     *       - the content to be sent
     * @return - an indication of success
     */
	public boolean sendAgentMessage(String agent, String content) {
		return centralMessagingShard.sendMessage(AgentWave.makePath(getName(), SHARD_ENDPOINT),
				AgentWave.makePath(agent, "messaging"), content);
	}
	
	@Override
	public boolean start() {
		if(centralMessagingShard == null) {
			le("[] unable to start. No messaging shard found.", getName());
			return false;
		}
		centralMessagingShard.register(name);
		isRunning = true;
		li("[] started successfully.", getName());
		return true;
	}
	
	@Override
	public boolean stop() {
		li("[] stopped successfully.", getName());
		isRunning = false;
		return true;
	}
	
	@Override
	public boolean isRunning() {
		return isRunning;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		PylonProxy pylonProxy = (PylonProxy) context;
		String recommendedShard = pylonProxy.getRecommendedShardImplementation(
				AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
		try {
			centralMessagingShard = (MessagingShard) PlatformUtils.getClassFactory().loadClassInstance(recommendedShard,
					null, true);
		} catch(ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException
				| InvocationTargetException e) {
			e.printStackTrace();
		}
		centralMessagingShard.addContext(proxy);
		return centralMessagingShard.addGeneralContext(context);
	}
	
	@Override
	public boolean removeContext(EntityProxy<Pylon> context) {
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return addContext((MessagingPylonProxy) context);
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return false;
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		return null;
	}
}

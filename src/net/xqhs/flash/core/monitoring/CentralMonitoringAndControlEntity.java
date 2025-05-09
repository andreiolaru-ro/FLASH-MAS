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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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
import net.xqhs.flash.gui.structure.GlobalConfiguration;
import net.xqhs.flash.web.WebEntity;
import net.xqhs.util.logging.Unit;

public class CentralMonitoringAndControlEntity extends Unit implements Entity<Pylon> {
	
	protected class EntityData {
		String		name;
		String		status;
		JSONArray	operations;
		Element		guiSpecification;
		
		public String getName() {
			return name;
		}
		
		public String getStatus() {
			return status;
		}
		
		public JSONArray getOperations() {
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
		public EntityData addOperations(JSONArray ops) {
			if(operations == null)
				operations = new JSONArray();
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
		public boolean postAgentEvent(AgentEvent event) {
			switch(event.getType()) {
			case AGENT_WAVE:
				return parseReceivedMsg((AgentWave) event);
			default:
				return false;
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
	 * Used for unknown entity names, statuses, etc.
	 */
	public static final String UNKNOWN = "none";
	
	private MessagingShard centralMessagingShard;
	
	private String name;
	
	private CentralGUI gui;
	
	private static boolean isRunning;
	
	/**
	 * Keeps track of all agents deployed in the system and their {@link List} of operations.
	 */
	private HashMap<String, List<String>> allAgents = new LinkedHashMap<>();
	
	/**
	 * Keeps track of all entities deployed in the system and their {@link JSONArray} of operations.
	 */
	private HashMap<String, JSONArray> entitiesToOp = new LinkedHashMap<>();
	
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
	
	public CentralMonitoringAndControlEntity(MultiTreeMap configuration) {
		this.name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		proxy = new CentralEntityProxy();
		standardCtrls = GUILoad.load(new MultiTreeMap().addOneValue("from", "controls.yml")
				.addOneValue(CategoryName.PACKAGE.s(), this.getClass().getPackage().getName()), getLogger());
		
		for(String iface : configuration.getValues(DeploymentConfiguration.CENTRAL_NODE_KEY))
			switch(iface) {
			case WEB_INTERFACE_SWITCH: {
				// TODO mock config -- to be added in deployment configuration?
				MultiTreeMap config = new MultiTreeMap().addOneValue("file", "interface-files/model-page/web-page.yml");
				GlobalConfiguration representation = null; // GUILoad.loadGlobalRepresentation(config);
				gui = new WebEntity(representation);
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
	 * @param obj
	 *            - the object received as content through the {@link ShardContainer}
	 * @param source
	 *            - the source of the message
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
			
			if(port.startsWith(CONTROL_OPERATIONS_PREFIX)) { // actually a control operation
				ControlOperation op = ControlOperation
						.fromOperation(port.substring(CONTROL_OPERATIONS_PREFIX.length()));
				AgentWave ctrlWave = new AgentWave(null, entity, ControlShard.SHARD_ENDPOINT, op.getOperation())
						.addSourceElements(SHARD_ENDPOINT);
				centralMessagingShard.sendMessage(ctrlWave.getCompleteSource(), ctrlWave.getCompleteDestination(),
						ctrlWave.getSerializedContent());
			}
			else { // normal input
				wave.removeFirstDestinationElement().recomputeCompleteDestination()
						.prependDestination(MonitoringOperation.GUI_INPUT_TO_ENTITY.getOperation())
						.prependDestination(MonitoringShard.SHARD_ENDPOINT).prependDestination(entity);
				wave.addSourceElementFirst(getName());
			}
			centralMessagingShard.sendMessage(wave.getCompleteSource(), wave.getCompleteDestination(),
					wave.getSerializedContent());
			return true;
		}
		Object obj = JSONValue.parse(content);
		if(obj == null) {
			le("null/unparsable message content from []: ", source, content);
			return false;
		}
		if(obj instanceof JSONObject) {
			JSONObject jo = (JSONObject) obj;
			if(manageOperation(jo)) {
				lf("Parsed operation from []: .", source, jo);
				return true;
			}
			else if(((String) jo.get("name")).equals("message")) {
				// WebEntity.agentMessages.put(source, ((String) jo.get("value")));
				lf("Parsed message from []: ", source, jo);
				return true;
			}
		}
		if(obj instanceof JSONArray) {
			JSONArray ja = (JSONArray) obj;
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
	private boolean manageOperation(JSONObject jsonObj) {
		String op = (String) jsonObj.get(OperationUtils.NAME);
		if(op.equals(MonitoringOperation.STATUS_UPDATE.getOperation())) {
			String params = (String) jsonObj.get(OperationUtils.PARAMETERS);
			String value = (String) jsonObj.get(OperationUtils.VALUE);
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
		}
		if(op.equals(MonitoringOperation.GUI_UPDATE.getOperation())) {
			String entity = (String) jsonObj.get(OperationUtils.PARAMETERS);
			Element interfaceStructure = GUILoad.fromYaml((String) jsonObj.get(OperationUtils.VALUE));
			if(interfaceStructure == null)
				try {
					interfaceStructure = (Element) standardCtrls.clone();
				} catch(CloneNotSupportedException e) {
					le("Failed to clone standard controls", e);
				}
			entitiesData.computeIfAbsent(entity, k -> new EntityData()).setGuiSpecification(interfaceStructure);
			try {
				entitiesData.get(entity).insertNewGuiElements(((Element) standardCtrls.clone()).getChildren());
			} catch(CloneNotSupportedException e) {
				e.printStackTrace();
			}
			return gui.updateGui(entity, interfaceStructure);
		}
		if(op.equals(MonitoringOperation.GUI_OUTPUT.getOperation())) {
			String entity = (String) jsonObj.get(OperationUtils.PARAMETERS);
			AgentWave wave;
			try {
				wave = (AgentWave) MultiValueMap.fromSerializedString((String) jsonObj.get(OperationUtils.VALUE));
			} catch(ClassNotFoundException e) {
				le("Unable to unpack AgentWave from ", entity);
				return false;
			}
			lf("The wave: ", wave);
			gui.sendOutput(wave);
			return true;
		}
		return false;
	}
	
	private boolean registerEntities(JSONArray ja) {
		for(Object o : ja) {
			JSONObject entity = (JSONObject) o;
			
			String node = (String) entity.get(OperationUtils.NODE);
			String category = (String) entity.get(OperationUtils.CATEGORY);
			String name = (String) entity.get(OperationUtils.NAME);
			
			JSONArray operationDetails = (JSONArray) entity.get(OperationUtils.OPERATIONS);
			if(category.equals("agent")) {
				if(!allAgents.containsKey(name))
					allAgents.put(name, new LinkedList<>());
				for(Object oo : operationDetails) {
					JSONObject op = (JSONObject) oo;
					String operation = (String) op.get(OperationUtils.NAME);
					allAgents.get(name).add(operation);
				}
			}
			entitiesToOp.put(name, operationDetails);
			
			if(!allNodeEntities.containsKey(node))
				allNodeEntities.put(node, new LinkedHashMap<>());
			if(!allNodeEntities.get(node).containsKey(category))
				allNodeEntities.get(node).put(category, new LinkedList<>());
			allNodeEntities.get(node).get(category).add(name);
			
			try {
				entitiesData.put(name, new EntityData().setName(name).setStatus(UNKNOWN).addOperations(operationDetails)
						.setGuiSpecification((Element) standardCtrls.clone()));
			} catch(CloneNotSupportedException e) {
				le("Failed to clone standard controls", e);
			}
			gui.updateGui(name, entitiesData.get(name).getGuiSpecification());
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
		JSONObject jsonOperation = getOperationFromEntity(destination, operation);
		if(jsonOperation == null) {
			le("Entity [] does not exist or does not support [] command.", destination, operation);
			return false;
		}
		String access = (String) jsonOperation.get("access");
		if(access.equals("self")) {
			JSONObject msg = OperationUtils.operationToJSON(operation, destination, "", destination);
			if(!sendMessage(destination, msg.toString())) {
				le("Message from [] to [] failed.", getName(), destination);
				return false;
			}
		}
		else {
			String proxy = (String) jsonOperation.get("proxy");
			JSONObject msg = OperationUtils.operationToJSON(operation, proxy, "", destination);
			if(!sendMessage(proxy, msg.toString())) {
				le("Message from [] to proxy [] of [] failed.", getName(), proxy, destination);
				return false;
			}
		}
		return true;
	}
	
	private JSONObject getOperationFromEntity(String entity, String command) {
		JSONArray ja = entitiesToOp.get(entity);
		if(ja == null)
			return null;
		for(Object o : ja) {
			JSONObject op = (JSONObject) o;
			String cmd = (String) op.get("name");
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

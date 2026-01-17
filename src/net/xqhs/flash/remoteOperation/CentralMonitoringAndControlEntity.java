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
package net.xqhs.flash.remoteOperation;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.Operation;
import net.xqhs.flash.core.util.Operation.BaseOperation;
import net.xqhs.flash.core.util.Operation.Field;
import net.xqhs.flash.core.util.Operation.OperationName;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.daemon.ControlClient;
import net.xqhs.flash.gui.GUILoad;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.web.WebEntity;

/**
 * This class is used to monitor and control the MAS.
 */
public class CentralMonitoringAndControlEntity extends EntityCore<Pylon> {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Operations supported by the {@link CentralMonitoringAndControlEntity}.
	 */
	@SuppressWarnings("hiding")
	public enum Operations implements OperationName {
		UPDATE_ENTITY_STATUS,
		REGISTER_ENTITIES,
		UPDATE_ENTITY_GUI,
		ENTITY_GUI_OUTPUT,
		GUI_INPUT_TO_ENTITY,
		DEPLOY_REMOTE,
		START_APPLICATION,
		STOP_APPLICATION,
		PAUSE_APPLICATION
		;

		/**
		 * If the first destination element is one of the supported operations, return it. Otherwise, return null.
		 *
		 * @param wave
		 * @return the operation, or null if the first destination element is not a supported operation.
		 */
		public static Operations getRoute(AgentWave wave) {
			try {
				return valueOf(wave.getFirstDestinationElement().toUpperCase());
			} catch(Exception e) {
				if (wave.getContent() != null && wave.getContent().toString().contains("DEPLOY_REMOTE")) {
					return DEPLOY_REMOTE;
				}
				return null;
			}
		}
	}

	public enum Fields implements Field {
		SPECIFICATION, RUNNING_STATUS, APPLICATION_STATUS, RUNNING_STATUS_RUNNING, RUNNING_STATUS_STOPPED, APPLICATION_STATUS_RUNNING, APPLICATION_STATUS_STOPPED, APPLICATION_STATUS_PAUSED, STATUS_UNKNOWN
	}

	public static final Operation  UPDATE_ENTITY_STATUS   = new BaseOperation(Operations.UPDATE_ENTITY_STATUS, Fields.RUNNING_STATUS, Fields.APPLICATION_STATUS);
	public static final Operation  UPDATE_ENTITY_GUI     = new BaseOperation(Operations.UPDATE_ENTITY_GUI, Fields.SPECIFICATION);
	public static final Operation  REGISTER_ENTITIES     = new BaseOperation(Operations.REGISTER_ENTITIES, (String[]) null);
	public static final Operation  ENTITY_GUI_OUTPUT     = new BaseOperation(Operations.ENTITY_GUI_OUTPUT, (String[]) null);
	public static final Operation  GUI_INPUT_TO_ENTITY       = new BaseOperation(Operations.GUI_INPUT_TO_ENTITY, (String[]) null);

	protected class EntityData {
		String entityName;
		String status;
		String appStatus;
		boolean    registered = false;
		Element    guiSpecification;
		String nodeName;

		public String getName() { return entityName; }
		public String getStatus() { return status; }
		public String getAppStatus() { return appStatus; }
		public Element getGuiSpecification() { return guiSpecification; }
		public String getNodeName() { return nodeName; }

		public EntityData setName(String name) { this.entityName = name; return this; }
		public EntityData setStatus(String status) { this.status = status; return this; }
		public EntityData setAppStatus(String appStatus) { this.appStatus = appStatus; return this; }
		public EntityData setGuiSpecification(Element guiSpecification) { this.guiSpecification = guiSpecification; return this; }
		public EntityData setNodeName(String nodeName) { this.nodeName = nodeName; return this; }

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
		public boolean postAgentEvent(AgentEvent event) {
			switch(event.getType()) {
				case AGENT_WAVE:
					return processWave((AgentWave) event);
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
	public static final String    WEB_INTERFACE_SWITCH      = "web";
	/**
	 * Use this in conjunction with {@link DeploymentConfiguration#CENTRAL_NODE_KEY} to switch on the swing interface.
	 */
	public static final String    SWING_INTERFACE_SWITCH    = "swing";
	/**
	 * The default port for the web interface.
	 */
	public static final int       WEB_INTERFACE_PORT        = 8080;
	/**
	 * Endpoint element for this shard.
	 */
	protected static final String ENTITY_STATUS_ELEMENT     = "standard-status";
	protected static final String ENTITY_APP_STATUS_ELEMENT = "standard-application-status";
	protected static final String ENTITY_LABEL_ELEMENT      = "standard-name";
	/**
	 * File for configuring the default controls for entities.
	 */
	protected static final String DEFAULT_CONTROLS          = "controls.yml";
	/**
	 * Standard controls for all entities.
	 */
	protected Element                             standardCtrls;
	/**
	 * The proxy to this entity.
	 */
	public ShardContainer                         centralProxy;
	/**
	 * Messaging shard for this entity.
	 */
	private MessagingShard                        centralMessagingShard;
	/**
	 * The GUI for controlling the deployment.
	 */
	private CentralGUI                            gui;
	/**
	 * Data for entities.
	 */
	protected Map<String, EntityData>             entitiesData   = new HashMap<>();
	/**
	 * Keeps track of all nodes deployed in the system, along with their {@link List} of entities, indexed by their
	 * categories and names.
	 */
	private HashMap<String, HashMap<String, List<String>>> allNodeEntities    = new LinkedHashMap<>();

	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		this.setUnitName("M&C");
		// this.setHighlighted();
		centralProxy = new CentralEntityProxy();
		standardCtrls = GUILoad.load(new MultiTreeMap().addOneValue(GUILoad.FILE_SOURCE_PARAMETER, DEFAULT_CONTROLS)
				.addOneValue(CategoryName.PACKAGE.s(), this.getClass().getPackage().getName()), getLogger());

		for(String iface : configuration.getValues(DeploymentConfiguration.CENTRAL_NODE_KEY)) {
			if(iface == null) {
				lw("No interface specified. Defaulting to swing.");
				iface = SWING_INTERFACE_SWITCH;
			}
			switch(iface) {
				case WEB_INTERFACE_SWITCH: {
					// TODO mock config -- to be added in deployment configuration?
					gui = new WebEntity(WEB_INTERFACE_PORT); // maybe TODO: move this port to a configuration file
					gui.addContext(centralProxy);
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
				default:
					return ler(false, "unknown central GUI type");
			}
		}
		return true;
	}

	/**
	 * Parses the received wave and calls the appropriate method.
	 *
	 * @param wave
	 *            - the {@link AgentWave} to be parsed
	 *
	 * @return - an indication of success
	 */
	public boolean processWave(AgentWave wave) {
		lf("Routing wave", wave);
		String sourceEntity = wave.getFirstSource();
		Operations op = Operations.getRoute(wave);

		if(op == null)
			return ler(false, "Unknown operation [] from [].", wave.getFirstDestinationElement(), wave.getCompleteSource());

		switch(op) {
			case DEPLOY_REMOTE:
				try {
					String content = wave.getContent().toString();
					li("M&C RECEIVED DEPLOY COMMAND: " + content);

					String targetIp = "";
					if (content.contains("\"targetIp\":\"")) {
						int start = content.indexOf("\"targetIp\":\"") + 12;
						int end = content.indexOf("\"", start);
						targetIp = content.substring(start, end);
					}

					String startupArgs = "";
					if (content.contains("\"startupArgs\":\"")) {
						int start = content.indexOf("\"startupArgs\":\"") + 15;
						int end = content.indexOf("\"", start);
						while(end < content.length() && content.charAt(end-1) == '\\') {
							end = content.indexOf("\"", end + 1);
						}
						startupArgs = content.substring(start, end);
						startupArgs = startupArgs.replace("\\\"", "\"");
					}

					String jarPath = "out/artifacts/Flash_MAS_jar/flash-mas.jar";

					final String finalIp = targetIp;
					final String finalArgs = startupArgs;
					final String finalJarPath = jarPath;

					if (!finalIp.isEmpty() && !finalArgs.isEmpty()) {
						new Thread(() -> {
							System.out.println(">>> Deploying to " + finalIp + " with args: " + finalArgs);
							ControlClient.deployAndStart(finalIp, 35274, finalJarPath, finalArgs);
						}).start();
					} else {
						System.err.println("Deployment skipped: Missing IP or Arguments.");
					}
					return true;
				} catch (Exception e) {
					System.err.println("Deployment Error: " + e.getMessage());
					e.printStackTrace();
					return false;
				}

			case REGISTER_ENTITIES:
				String node = sourceEntity;
				if(!allNodeEntities.containsKey(node))
					allNodeEntities.put(node, new LinkedHashMap<>());
				for(String entityName : wave.getContentElements()) {
					String category = wave.get(entityName);
					if(!allNodeEntities.get(node).containsKey(category))
						allNodeEntities.get(node).put(category, new LinkedList<>());
					allNodeEntities.get(node).get(category).add(entityName);

					if(!entitiesData.containsKey(entityName))
						entitiesData.put(entityName, new EntityData().setName(entityName));
					EntityData ed = entitiesData.get(entityName);
					ed.registered = true;
					ed.setNodeName(node);

					if(ed.getStatus() == null) {
						ed.setStatus(Fields.STATUS_UNKNOWN.name());
						ed.setAppStatus(Fields.STATUS_UNKNOWN.name());
					}
					Element standardControls = setupStandardControls(ed.getStatus(), ed.getAppStatus(), entityName);
					if(ed.getGuiSpecification() == null) {
						ed.setGuiSpecification(standardControls);
					}
					else {
						Element guiSpec = ed.getGuiSpecification();
						for(Element child : standardControls.getChildren()) {
							if(!guiSpec.getChildren().contains(child))
								guiSpec.addChild(child);
						}
					}
					li("Registered entity []/[] in []", category, entityName, node);

					gui.updateGui(entityName, ed.getGuiSpecification());
				}
				return true;

			case UPDATE_ENTITY_STATUS:
				li("UPDATE_ENTITY_STATUS wave: []", wave);
				Fields entityStatus = (Fields) wave.getObject(Fields.RUNNING_STATUS.name(), Fields.STATUS_UNKNOWN);
				Fields appStatus = (Fields) wave.getObject(Fields.APPLICATION_STATUS.name(), Fields.STATUS_UNKNOWN);

				if(!entitiesData.containsKey(sourceEntity) || !entitiesData.get(sourceEntity).registered)
					lw("Entity [] not yet registered when [].", sourceEntity, op);

				entitiesData.computeIfAbsent(sourceEntity, (k) -> new EntityData().setName(sourceEntity))
						.setStatus(entityStatus.name()).setAppStatus(appStatus.name());

				li("Status update for []: [], []", sourceEntity, entityStatus, appStatus);
				return gui.sendOutput(new AgentWave(entityStatus.name(), sourceEntity, ENTITY_STATUS_ELEMENT))
						&& gui.sendOutput(new AgentWave(appStatus.name(), sourceEntity, ENTITY_APP_STATUS_ELEMENT));

			case UPDATE_ENTITY_GUI:
				Element interfaceStructure = (Element) wave.getObject(Fields.SPECIFICATION.name());
				Element interfaceContainer = new Element();
				if(interfaceStructure != null)
					// must avoid adding it twice
					for(Element child : interfaceStructure.getChildren())
						if(!interfaceContainer.getChildren().contains(child))
							interfaceContainer.addChild(child);
				if(!entitiesData.containsKey(sourceEntity) || !entitiesData.get(sourceEntity).registered)
					lw("Entity [] not yet registered when [].", sourceEntity, op);
				else
					interfaceContainer.addAllChildren(setupStandardControls(entitiesData.get(sourceEntity).getStatus(),
							entitiesData.get(sourceEntity).getAppStatus(), entitiesData.get(sourceEntity).getName())
							.getChildren());
				entitiesData.computeIfAbsent(sourceEntity, (k) -> new EntityData().setName(sourceEntity))
						.setGuiSpecification(interfaceContainer);
				lf("Interface of [] reset to:", sourceEntity, interfaceContainer);
				return gui.updateGui(sourceEntity, interfaceContainer);

			case ENTITY_GUI_OUTPUT:
				// remove the name of Central; add the entity sending the output
				return gui.sendOutput(wave.removeFirstDestinationElement().prependDestination(sourceEntity)
						.recomputeCompleteDestination());

			case GUI_INPUT_TO_ENTITY:
				li("GUI input to entity []: []", sourceEntity, wave.toString());
				wave.removeFirstDestinationElement();

				String[] sourceElements = wave.getSourceElements(); // source is gui/entity/port/role
				String entityName = sourceElements[1];
				String sourcePort = sourceElements[2], sourceRole = sourceElements[3];
				EntityData entityData = entitiesData.get(entityName);

				Element guiSpecification = entityData.getGuiSpecification();
				Element sourceElement = guiSpecification.getChild(sourcePort, sourceRole);
				HashMap<String, String> elementProperties = sourceElement.getProperties();
				if("node".equals(elementProperties.getOrDefault("proxy", "")))
					wave.prependDestination(entityData.getNodeName());

				wave.recomputeCompleteDestination();
				wave.addSourceElementFirst(getName());
				return centralMessagingShard.sendMessage(wave);

			case START_APPLICATION:
				li("Broadcasting START to all agents.");
				broadcastCommandToAgents("START_APPLICATION");
				return true;

			case STOP_APPLICATION:
				li("Broadcasting STOP to all agents.");
				broadcastCommandToAgents("STOP_APPLICATION");
				return true;

			case PAUSE_APPLICATION:
				li("Broadcasting PAUSE to all agents.");
				broadcastCommandToAgents("PAUSE_APPLICATION");
				return true;

			default:
				lw("Unhandled operation [] from [].", wave.getFirstDestinationElement(), wave.getCompleteSource());
				return false;
		}
	}

	/**
	 * Sets up the standard controls for an entity.
	 *
	 * @param status
	 *            - the status of the entity
	 * @param appStatus
	 *            - the application status for the entity
	 * @param entityName
	 *            - the name of the entity
	 * @return an {@link Element} containing the standard controls for the entity
	 */
	protected Element setupStandardControls(String status, String appStatus, String entityName) {
		Element element = (Element) standardCtrls.clone();
		if(!element.getChildren(ENTITY_LABEL_ELEMENT).isEmpty())
			element.getChildren(ENTITY_LABEL_ELEMENT).get(0).setValue(entityName);
		if(!element.getChildren(ENTITY_STATUS_ELEMENT).isEmpty())
			element.getChildren(ENTITY_STATUS_ELEMENT).get(0).setValue(status);
		if(!element.getChildren(ENTITY_APP_STATUS_ELEMENT).isEmpty())
			element.getChildren(ENTITY_APP_STATUS_ELEMENT).get(0).setValue(appStatus);
		return element;
	}

	@Override
	public boolean start() {
		if(!super.start())
			return false;
		if(centralMessagingShard == null) {
			le("[] unable to start. No messaging shard found.", getName());
			return false;
		}
		// TODO hack
		// Timer timer = new Timer();
		// timer.schedule(new TimerTask() {
		// @Override
		// public void run() {
		// centralMessagingShard.register(name);
		// }
		// }, 1000);
		centralMessagingShard.register(name);
		li("[] started successfully.", getName());
		return true;
	}

	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		PylonProxy pylonProxy = (PylonProxy) context;
		String recommendedShard = pylonProxy.getRecommendedShardImplementation(
				AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING));
		try {
			centralMessagingShard = (MessagingShard) PlatformUtils.getClassFactory().loadClassInstance(recommendedShard, null, true);
		} catch(ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
		centralMessagingShard.addContext(centralProxy);
		return centralMessagingShard.addGeneralContext(context);
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if (context instanceof MessagingPylonProxy) {
			return addContext((MessagingPylonProxy) context);
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
		return (EntityProxy<C>) centralProxy;
	}

	private void broadcastCommandToAgents(String commandName) {
		for (String node : allNodeEntities.keySet()) {
			HashMap<String, List<String>> categories = allNodeEntities.get(node);
			for (String category : categories.keySet()) {
				if ("agent".equals(category) || "mobileComposite".equals(category)) {
					for (String agentName : categories.get(category)) {
						AgentWave wave = new AgentWave();
						wave.resetDestination(agentName, "remote", commandName);
						wave.addSourceElements(getName());

						if (centralMessagingShard != null) {
							centralMessagingShard.sendMessage(wave);
						}
					}
				}
			}
		}
	}
}
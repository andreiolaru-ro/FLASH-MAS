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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import net.xqhs.flash.daemon.FlashMasDaemon;
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
		PAUSE_APPLICATION,
		CLIENT_CONNECTED,
		SEND_NODE_CONFIGS,
		CONNECT_DAEMON,
		RESET_DAEMON_STATUSES,
		GET_DAEMONS_LIST,
		KILL_NODE_REMOTE,
		KILL_DAEMON_REMOTE,
		KILL_ALL_JVMS,
		KILL_ALL_DAEMONS,
		KEEP_ALIVE
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
				return null;
			}
		}
	}

	/**
	 * Fields used in operations.
	 */
	public enum Fields implements Field {
		SPECIFICATION, RUNNING_STATUS, APPLICATION_STATUS, RUNNING_STATUS_RUNNING, RUNNING_STATUS_STOPPED, APPLICATION_STATUS_RUNNING, APPLICATION_STATUS_STOPPED, APPLICATION_STATUS_PAUSED, STATUS_UNKNOWN
	}

	/**
	 * Operation definition for updating entity status.
	 */
	public static final Operation  UPDATE_ENTITY_STATUS   = new BaseOperation(Operations.UPDATE_ENTITY_STATUS, Fields.RUNNING_STATUS, Fields.APPLICATION_STATUS);
	/**
	 * Operation definition for updating entity GUI specification.
	 */
	public static final Operation  UPDATE_ENTITY_GUI     = new BaseOperation(Operations.UPDATE_ENTITY_GUI, Fields.SPECIFICATION);
	/**
	 * Operation definition for registering entities.
	 */
	public static final Operation  REGISTER_ENTITIES     = new BaseOperation(Operations.REGISTER_ENTITIES, (String[]) null);
	/**
	 * Operation definition for handling GUI output from an entity.
	 */
	public static final Operation  ENTITY_GUI_OUTPUT     = new BaseOperation(Operations.ENTITY_GUI_OUTPUT, (String[]) null);
	/**
	 * Operation definition for handling input from the GUI to an entity.
	 */
	public static final Operation  GUI_INPUT_TO_ENTITY       = new BaseOperation(Operations.GUI_INPUT_TO_ENTITY, (String[]) null);
	/**
	 * Operation definition for keeping alive entities.
	 */
	public static final Operation  KEEP_ALIVE       = new BaseOperation(Operations.KEEP_ALIVE, (String[]) null);

	/**
	 * Configuration key for specifying remote daemons.
	 */
	public static final String CONFIG_DAEMONS_KEY = "remote.daemons";

	/**
	 * Interval in seconds for pinging daemons to check connectivity.
	 */
	protected static final int DAEMON_PING_INTERVAL_SECONDS = 10;

	/**
	 * Default path to the JAR file to be deployed.
	 */
	private final String defaultJarPath = "out/artifacts/Flash_MAS_jar/Flash-MAS.jar";

	/**
	 * Information structure for tracking remote daemons.
	 */
	public static class DaemonInfo {
		public String ip;
		public int port;
		public ControlClient.RemoteStatus status;
		public boolean jarUploaded = false;
		public boolean isDeployed = false;

		/**
		 * Creates a new DaemonInfo instance.
		 * @param ip the IP address of the daemon.
		 * @param port the port number of the daemon.
		 */
		public DaemonInfo(String ip, int port) {
			this.ip = ip;
			this.port = port;
			this.status = ControlClient.RemoteStatus.UNREACHABLE;
		}

		/**
		 * Gets the unique key for this daemon.
		 * @return a string key in the format "ip:port".
		 */
		public String getKey() { return ip + ":" + port; }
	}

	/**
	 * Map of known daemons indexed by their unique key (ip:port).
	 */
	private final Map<String, DaemonInfo> knownDaemons = new ConcurrentHashMap<>();

	/**
	 * Scheduler for background tasks like pinging daemons.
	 */
	private ScheduledExecutorService pingScheduler;

	/**
	 * Scheduler for background tasks like pinging agents.
	 */
	private ScheduledExecutorService agentKeepAliveScheduler;

	/**
	 * Internal structure to hold state and configuration for registered entities.
	 */
	protected class EntityData {
		String entityName;
		String status;
		String appStatus;
		boolean    registered = false;
		Element    guiSpecification;
		String nodeName;
		long lastKeepAliveTime = System.currentTimeMillis();

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
	/**
	 * Default configurations for nodes.
	 */
	protected Map<String, String> defaultNodeConfigurations = new HashMap<>();

	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		this.setUnitName("M&C");
		// this.setHighlighted();
		centralProxy = new CentralEntityProxy();
		standardCtrls = GUILoad.load(new MultiTreeMap().addOneValue(GUILoad.FILE_SOURCE_PARAMETER, DEFAULT_CONTROLS)
				.addOneValue(CategoryName.PACKAGE.s(), this.getClass().getPackage().getName()), getLogger());
		String defaultArgs = "-loader agent:composite -node nodeC -pylon webSocket:clientPylon connectTo:ws://127.0.0.1:8886 -agent composite:AgentC -shard messaging -shard remoteOperation -shard swingGui from:basic-chat.yml -shard test.guiGeneration.BasicChatShard otherAgent:AgentA";

		defaultNodeConfigurations.put("127.0.0.1", defaultArgs);
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
		if (configuration.containsKey(CONFIG_DAEMONS_KEY)) {
			for (String daemonStr : configuration.getValues(CONFIG_DAEMONS_KEY)) {
				try {
					String[] parts = daemonStr.split(":");
					String ip = parts[0];
					int port = parts.length > 1 ? Integer.parseInt(parts[1]) : FlashMasDaemon.DEFAULT_PORT;

					li("Config: Scheduling connection to daemon {}:{}", ip, port);
					new Thread(() -> performConnect(ip, port)).start();

				} catch (Exception e) {
					le("Invalid daemon config format: " + daemonStr);
				}
			}
		}
		return true;
	}

	/**
	 * Helper to perform "Connect": Add to list + Upload JAR.
	 * @param ip the IP address of the remote daemon.
	 * @param port the port number of the remote daemon.
	 */
	private void performConnect(String ip, int port) {
		String key = ip + ":" + port;
		DaemonInfo info = knownDaemons.computeIfAbsent(key, k -> new DaemonInfo(ip, port));

		info.status = ControlClient.checkRemoteStatus(ip, port);

		if (info.status == ControlClient.RemoteStatus.ONLINE) {
			li("Daemon {}:{} is ONLINE. Attempting JAR Upload...", ip, port);
			boolean uploaded = ControlClient.uploadJar(ip, port, defaultJarPath);
			info.jarUploaded = uploaded;
			if (uploaded) li("Initial Upload to {}:{} SUCCESS.", ip, port);
			else le("Initial Upload to {}:{} FAILED.", ip, port);
		} else {
			le("Daemon {}:{} is UNREACHABLE. Added to list, will retry.", ip, port);
		}

		pushDaemonListToGui();
	}

	/**
	 * Parses the received wave and calls the appropriate method.
	 *
	 * @param wave
	 * - the {@link AgentWave} to be parsed
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
			case KEEP_ALIVE:
				if(entitiesData.containsKey(sourceEntity)) {
					entitiesData.get(sourceEntity).lastKeepAliveTime = System.currentTimeMillis();
					
					// Daca agentul era INACTIVE si si-a revenit, il marcam inapoi RUNNING. Statusul lui real
					// ar trebui sa vina de la el, dar macar stergem semnalizarea rosie.
					if ("INACTIVE".equals(entitiesData.get(sourceEntity).getStatus())) {
						entitiesData.get(sourceEntity).setStatus(Fields.RUNNING_STATUS_RUNNING.name());
						entitiesData.get(sourceEntity).setAppStatus(Fields.RUNNING_STATUS_RUNNING.name());
						if(gui != null) {
							gui.sendOutput(new AgentWave(Fields.RUNNING_STATUS_RUNNING.name(), sourceEntity, ENTITY_STATUS_ELEMENT));
							gui.sendOutput(new AgentWave(Fields.RUNNING_STATUS_RUNNING.name(), sourceEntity, ENTITY_APP_STATUS_ELEMENT));
							if (gui instanceof WebEntity) {
								WebEntity webGui = (WebEntity) gui;
								webGui.sendToClient(WebEntity.buildMessage("global", "entities list", webGui.getEntities()));
							}
						}
					}
				}
				return true;
			case DEPLOY_REMOTE:
				try {
					String content = wave.getContent().toString();
					li("M&C RECEIVED DEPLOY COMMAND: " + content);

					JsonObject jsonObject = JsonParser.parseString(content).getAsJsonObject();

					String targetIp = "";
					if (jsonObject.has("targetIp") && !jsonObject.get("targetIp").isJsonNull()) {
						targetIp = jsonObject.get("targetIp").getAsString();
					}

					String startupArgs = "";
					if (jsonObject.has("startupArgs") && !jsonObject.get("startupArgs").isJsonNull()) {
						startupArgs = jsonObject.get("startupArgs").getAsString();
					}

					int targetPort = FlashMasDaemon.DEFAULT_PORT;
					if (jsonObject.has("port") && !jsonObject.get("port").isJsonNull()) {
						try {
							targetPort = jsonObject.get("port").getAsInt();
						} catch (NumberFormatException e) {
							lw("Invalid port in JSON, using default: {}", FlashMasDaemon.DEFAULT_PORT);
						}
					}

					final String finalIp = targetIp;
					final String finalArgs = startupArgs;
					final String finalJarPath = defaultJarPath;
					final int finalPort = targetPort;

					if (!finalIp.isEmpty() && !finalArgs.isEmpty()) {

						DaemonInfo dInfo = knownDaemons.computeIfAbsent(finalIp + ":" + finalPort, k -> new DaemonInfo(finalIp, finalPort));

						new Thread(() -> {
							li(">>> Processing Deployment for " + finalIp + ":" + finalPort);

							if (!dInfo.jarUploaded) {
								li(">>> JAR not marked as uploaded. Uploading now...");
								boolean uploaded = ControlClient.uploadJar(finalIp, finalPort, finalJarPath);
								dInfo.jarUploaded = uploaded;
								pushDaemonListToGui();
							}

							if (dInfo.jarUploaded) {
								li(">>> Starting Node on " + finalIp + "...");
								boolean started = ControlClient.startNode(finalIp, finalPort, finalArgs);

								if (started) {
									dInfo.isDeployed = true;
									li(">>> Node successfully started on " + finalIp);
								} else {
									le(">>> Failed to start node on " + finalIp);
								}

								pushDaemonListToGui();
							} else {
								le(">>> Cannot start node. Upload failed.");
							}
						}).start();

					} else {
						le("Deployment skipped: Missing IP or Arguments in JSON.");
					}
					return true;
				} catch (Exception e) {
					le("Deployment JSON Error: " + e.getMessage());
					e.printStackTrace();
					return false;
				}
			case KILL_ALL_JVMS:
				li(">>> Received command: KILL ALL JVMs");
				for (DaemonInfo info : knownDaemons.values()) {
					if (info.status == ControlClient.RemoteStatus.ONLINE) {
						new Thread(() -> {
							li("Stopping JVM at " + info.ip);
							boolean success = ControlClient.killRemoteNode(info.ip, info.port);
							if (success) {
								info.isDeployed = false;
								String safeIp = info.ip.replace('.', '_');
								removeNode("node_" + safeIp);
								removeNode(info.ip);
							}
						}).start();
					}
				}
				new Thread(() -> {
					try { Thread.sleep(500); } catch (Exception e){}
					pushDaemonListToGui();
				}).start();
				return true;
			case KILL_ALL_DAEMONS:
				li(">>> Received command: KILL ALL DAEMONS");
				for (DaemonInfo info : knownDaemons.values()) {
					new Thread(() -> {
						li("Stopping Daemon at " + info.ip);
						boolean success = ControlClient.killRemoteDaemon(info.ip, info.port);
						info.status = ControlClient.RemoteStatus.UNREACHABLE;
						info.isDeployed = false;

						if (success) {
							String safeIp = info.ip.replace('.', '_');
							removeNode("node_" + safeIp);
							removeNode(info.ip);
						}
					}).start();
				}
				new Thread(() -> {
					try { Thread.sleep(500); } catch (Exception e){}
					pushDaemonListToGui();
				}).start();
				return true;
			case KILL_NODE_REMOTE:
				handleKillCommand(wave, false);
				return true;

			case KILL_DAEMON_REMOTE:
				handleKillCommand(wave, true);
				return true;
			case CONNECT_DAEMON:
				try {
					String conContent = wave.getContent().toString();
					JsonObject conJson = JsonParser.parseString(conContent).getAsJsonObject();

					if (!conJson.has("ip")) {
						le("Connect command missing IP address.");
						return false;
					}

					String conIp = conJson.get("ip").getAsString();
					int conPort = conJson.has("port") ? conJson.get("port").getAsInt() : FlashMasDaemon.DEFAULT_PORT;

					li("Manually connecting to daemon: " + conIp + ":" + conPort);
					new Thread(() -> performConnect(conIp, conPort)).start();
					return true;
				} catch (Exception e) {
					le("Error parsing CONNECT_DAEMON: " + e.getMessage());
					return false;
				}

			case RESET_DAEMON_STATUSES:
				li("Resetting all Daemon upload statuses.");
				for(DaemonInfo di : knownDaemons.values()) {
					di.jarUploaded = false;
				}
				pushDaemonListToGui();
				return true;
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
				if (gui instanceof WebEntity) {
					WebEntity webGui = (WebEntity) gui;
					webGui.sendToClient(WebEntity.buildMessage("global", "entities list", webGui.getEntities()));
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
				boolean updateResult = gui.updateGui(sourceEntity, interfaceContainer);
				if (gui instanceof WebEntity) {
					WebEntity webGui = (WebEntity) gui;
					webGui.sendToClient(WebEntity.buildMessage("global", "entities list", webGui.getEntities()));
				}
				return updateResult;
			case ENTITY_GUI_OUTPUT:
				// remove the name of Central; add the entity sending the output
				return gui.sendOutput(wave.removeFirstDestinationElement().prependDestination(sourceEntity)
						.recomputeCompleteDestination());

			case GUI_INPUT_TO_ENTITY:
				li("GUI input to entity []: []", sourceEntity, wave.toString());
				wave.removeFirstDestinationElement();

				String[] sourceElements = wave.getSourceElements(); // source is gui/entity/port/role
				String entityName = sourceElements[1];
				String sourcePort = sourceElements[2];
				String sourceRole = sourceElements[3];

				EntityData entityData = entitiesData.get(entityName);

				Element guiSpecification = entityData.getGuiSpecification();
				Element sourceElement = guiSpecification.getChild(sourcePort, sourceRole);
				HashMap<String, String> elementProperties = sourceElement.getProperties();

				String proxyTarget = elementProperties.getOrDefault("proxy", "");

				wave.recomputeCompleteDestination();
				wave.addSourceElementFirst(getName());

				if ("node".equals(proxyTarget) && !("standard-stop".equals(sourcePort) || "standard-start".equals(sourcePort))) {
					String nodeToKill = entityData.getNodeName();
					wave.prependDestination(nodeToKill);

					return centralMessagingShard.sendMessage(wave);
				} else {
					wave.prependDestination("remote");
					wave.prependDestination(entityName);

					if ("standard-stop".equals(sourcePort)) {
						wave.resetDestination(entityName, "remote", RemoteOperationShard.Operations.REMOTE_STOP.name());
					} else if ("standard-start".equals(sourcePort)) {
						entitiesData.get(entityName).setStatus(Fields.RUNNING_STATUS_RUNNING.name());
						entitiesData.get(entityName).setAppStatus(Fields.RUNNING_STATUS_RUNNING.name());
						
						wave.resetDestination(entityName, "remote", RemoteOperationShard.Operations.START_APPLICATION.name());
					}

					boolean sent = centralMessagingShard.sendMessage(wave);

					// Daca oprim agentul, vrem sa ii si schimbam statusul in UI rapid, desi el ar trebui sa raporteze singur.
					// Dar ca si backup.
					return sent;
				}

			case CLIENT_CONNECTED:
				li("Web client connected. Sending default node configurations.");

				AgentWave configWave = new AgentWave();
				configWave.resetDestination(Operations.SEND_NODE_CONFIGS.name());

				for (Map.Entry<String, String> entry : defaultNodeConfigurations.entrySet()) {
					configWave.add(entry.getKey(), entry.getValue());
				}

				return gui.sendOutput(configWave);
			case GET_DAEMONS_LIST:
				pushDaemonListToGui();
				return true;
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
	 * - the status of the entity
	 * @param appStatus
	 * - the application status for the entity
	 * @param entityName
	 * - the name of the entity
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
		startDaemonPingTask();
		startAgentKeepAliveCheckTask();
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

	/**
	 * Updates the GUI (if available) with the current list of known daemons.
	 */
	private void pushDaemonListToGui() {
		if (gui instanceof WebEntity) {
			((WebEntity) gui).sendDaemonList(knownDaemons.values());
		}
	}

	/**
	 * Starts the background task to ping daemons.
	 */
	private void startDaemonPingTask() {
		pingScheduler = Executors.newSingleThreadScheduledExecutor();
		pingScheduler.scheduleAtFixedRate(() -> {
			try {
				if (knownDaemons.isEmpty()) return;

				boolean changed = false;
				for (DaemonInfo daemon : knownDaemons.values()) {
					ControlClient.RemoteStatus oldStatus = daemon.status;
					ControlClient.RemoteStatus newStatus = ControlClient.checkRemoteStatus(daemon.ip, daemon.port);

					daemon.status = newStatus;

					if (oldStatus != newStatus) {
						changed = true;
						li("Daemon status changed: {}:{} -> {}", daemon.ip, daemon.port, newStatus);
					}
				}

				if (changed && gui != null) {
					pushDaemonListToGui();
				}

			} catch (Exception e) {
				le("Error in Daemon Ping Loop", e);
			}
		}, 2, DAEMON_PING_INTERVAL_SECONDS, TimeUnit.SECONDS);
	}

	private void startAgentKeepAliveCheckTask() {
		agentKeepAliveScheduler = Executors.newSingleThreadScheduledExecutor();
		agentKeepAliveScheduler.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			boolean changed = false;

			for (Map.Entry<String, EntityData> entry : entitiesData.entrySet()) {
				EntityData ed = entry.getValue();
				String entityName = ed.getName();

				boolean isAgent = false;
				for (HashMap<String, List<String>> categories : allNodeEntities.values()) {
					if (categories.getOrDefault("agent", new LinkedList<>()).contains(entityName) ||
						categories.getOrDefault("mobileComposite", new LinkedList<>()).contains(entityName)) {
						isAgent = true;
						break;
					}
				}

				if (!isAgent) continue;

				if (now - ed.lastKeepAliveTime > 10000) {
					if (!"INACTIVE".equals(ed.getStatus())) {
						ed.setStatus("INACTIVE");
						ed.setAppStatus("INACTIVE");
						changed = true;

						if(gui != null) {
							gui.sendOutput(new AgentWave("INACTIVE", ed.getName(), ENTITY_STATUS_ELEMENT));
							gui.sendOutput(new AgentWave("INACTIVE", ed.getName(), ENTITY_APP_STATUS_ELEMENT));
						}
					}
				}
			}

			if (changed && gui instanceof WebEntity) {
				WebEntity webGui = (WebEntity) gui;
				webGui.sendToClient(WebEntity.buildMessage("global", "entities list", webGui.getEntities()));
			}
		}, 5, 5, TimeUnit.SECONDS);
	}

	/**
	 * Removes a node from the records and updates the GUI.
	 * * @param nodeName the name of the node to remove.
	 */
	private void removeNode(String nodeName) {
		if (allNodeEntities.containsKey(nodeName)) {
			li("Removing entities for killed node: " + nodeName);
			HashMap<String, List<String>> categories = allNodeEntities.get(nodeName);
			if (categories != null) {
				for (List<String> entityList : categories.values()) {
					for (String en : entityList) {
						entitiesData.remove(en);
						if (gui instanceof WebEntity) {
							((WebEntity) gui).removeEntityGui(en);
						}
					}
				}
			}
			allNodeEntities.remove(nodeName);

			if (gui instanceof WebEntity) {
				WebEntity webGui = (WebEntity) gui;
				webGui.sendToClient(WebEntity.buildMessage("global", "entities list", webGui.getEntities()));
			}
		}
	}

	/**
	 * Handles a kill command received from the UI.
	 * @param wave the agent wave containing the command parameters.
	 * @param killDaemon true if the command is to kill the daemon, false to kill the node (JVM).
	 */
	private void handleKillCommand(AgentWave wave, boolean killDaemon) {
		try {
			JsonObject json = JsonParser.parseString(wave.getContent().toString()).getAsJsonObject();
			String ip = json.get("ip").getAsString();
			int port = json.get("port").getAsInt();

			DaemonInfo info = knownDaemons.get(ip + ":" + port);

			new Thread(() -> {
				boolean success;
				if (killDaemon) {
					success = ControlClient.killRemoteDaemon(ip, port);
					if (success) {
						if(info != null) {
							info.status = ControlClient.RemoteStatus.UNREACHABLE;
							info.isDeployed = false;
						}
					}
				} else {
					success = ControlClient.killRemoteNode(ip, port);
					if (success) {
						if(info != null) info.isDeployed = false;
					}
				}

				if (success) {
					String safeIp = ip.replace('.', '_');
					removeNode("node_" + safeIp);
					removeNode(ip);
				}

				pushDaemonListToGui();
			}).start();
		} catch (Exception e) {
			le("Kill command failed", e);
		}
	}

	/**
	 * Broadcasts a global command to all known agents.
	 * @param commandName the name of the command to broadcast.
	 */
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

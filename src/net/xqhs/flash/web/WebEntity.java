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
package net.xqhs.flash.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.Json;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.OperationUtils.MonitoringOperation;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.gui.structure.ElementIdManager;
import net.xqhs.flash.remoteOperation.CentralGUI;
import net.xqhs.flash.remoteOperation.CentralMonitoringAndControlEntity;
import net.xqhs.flash.remoteOperation.CentralMonitoringAndControlEntity.CentralEntityProxy;

/**
 * Web entity for the connection between the MAS and the web interface.
 */
public class WebEntity extends CentralGUI {
	
	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -8088098471516262577L;
	
	/** Address for ws communication from server to client */
	protected static final String SERVER_TO_CLIENT = "server-to-client";
	
	/** Address for ws communication from client to server */
	protected static final String CLIENT_TO_SERVER = "client-to-server";
	
	/** The endpoint for ws communication */
	protected static final String WS_ENDPOINT = "/eventbus";

	/** The scope for server messages regarding updates to a port */
	protected static final String PORT_SCOPE = "port";

	/** The scope for client messages regarding a client registration */
	protected static final String REGISTERED_SCOPE = "registered";

	/** The scope for client messages regarding a notification */
	protected static final String NOTIFY_SCOPE = "notify";

	/** The scope key for a message from the client */
	protected static final String MESSAGE_SCOPE = "scope";

	/** The subject key for a message from the client */
	protected static final String MESSAGE_SUBJECT = "subject";

	/** The content key for a message from the client */
	protected static final String MESSAGE_CONTENT = "content";

	/** The port on which the HTTP server is running */
	protected int httpPort;

	/** A promise that is completed when a client connection is fully established */
	protected Promise<Void> running = Promise.promise();

	/** The Gson object used for JSON serialization and deserialization */
	protected static final Gson gson = new Gson();

	/**
	 * Constructor for the web entity.
	 * @param port - the port the HTTP server is started on.
	 */
	public WebEntity(int port) {
		super();
		this.httpPort = port;
	}
	
	/**
	 * The server verticle for the web entity.
	 */
	class ServerVerticle extends AbstractVerticle {
		/**
		 * The web entity this verticle is associated with.
		 */
		private WebEntity entity;
		
		/**
		 * Constructor for the server verticle.
		 * 
		 * @param entity
		 *            - the web entity this verticle is associated with.
		 */
		public ServerVerticle(WebEntity entity) {
			this.entity = entity;
		}
		
		@Override
		public void start(Promise<Void> startPromise) throws Exception {
			Router router = Router.router(vertx);
			SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
			BridgeOptions options = new BridgeOptions()
					.addOutboundPermitted(new PermittedOptions().setAddress(SERVER_TO_CLIENT))
					.addInboundPermitted(new PermittedOptions().setAddress(CLIENT_TO_SERVER));
			
			// mount the bridge on the router
			router.mountSubRouter(WS_ENDPOINT, sockJSHandler.bridge(options, bridgeEvent -> {
				try {
					if (bridgeEvent.type() == BridgeEventType.SOCKET_CREATED) {
						System.out.println("created");
					}
					else if (bridgeEvent.type() == BridgeEventType.REGISTER) {
						System.out.println("register");
					}
					else if (bridgeEvent.type() == BridgeEventType.UNREGISTER) {
						System.out.println("unregister");
						entity.running = Promise.promise();
					}
					else if (bridgeEvent.type() == BridgeEventType.SOCKET_CLOSED) {
						System.out.println("closed");
					}
					else if (bridgeEvent.type() == BridgeEventType.SEND) {
						System.out.println("client-message");
					}
					else if (bridgeEvent.type() == BridgeEventType.RECEIVE) {
						System.out.println("server-message");
					}
				} catch (Exception e) {
					// do nothing
				} finally {
					bridgeEvent.complete(Boolean.valueOf(true));
				}
			}));
			
			// handle messages from the client
			vertx.eventBus().consumer(CLIENT_TO_SERVER).handler(objectMessage -> {
				// convert from vertx.json.JsonObject to Gson JsonObject
				JsonObject msg = JsonParser.parseString(objectMessage.body().toString()).getAsJsonObject();
				String scope = msg.get(MESSAGE_SCOPE).getAsString();

				if (REGISTERED_SCOPE.equals(scope)) {
					JsonObject entities = getEntities();
					String message = buildMessage("global", "entities list", entities);
					vertx.eventBus().send(SERVER_TO_CLIENT, message);
					entity.running.complete();
				}
				else if (NOTIFY_SCOPE.equals(scope)) {
					JsonArray subject = msg.get(MESSAGE_SUBJECT).getAsJsonArray();
					JsonObject content = msg.get(MESSAGE_CONTENT).getAsJsonObject();
					entity.sendNotification(subject, content);
				}
			});
			
			System.out.println(WebEntity.class.getPackage().getName());
			String pkg = DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[0] + "/"
					+ WebEntity.class.getPackage().getName().replace(".", "/");
			
			router.route().handler(StaticHandler.create(pkg).setIndexPage("page.html").setCachingEnabled(false));
			
			vertx.createHttpServer().requestHandler(router).listen(httpPort, http -> {
				if (http.succeeded()) {
					entity.li("HTTP server started on port []", Integer.valueOf(httpPort));
					startPromise.complete();
				} else {
					entity.li("HTTP server failed to start on port []", Integer.valueOf(httpPort));
					startPromise.fail(http.cause());
				}
			});
		}
		
		@Override
		public void stop(Promise<Void> stopPromise) throws Exception {
			System.out.println("HTTP server stoped");
			stopPromise.complete();
		}
	}
	
	static CentralEntityProxy cep;
	
	// public static JsonObject agentMessages;
	
	private Element specification;
	
	protected ElementIdManager idManager = new ElementIdManager();
	
	private Vertx web;
	
	// protected boolean verticleReady = false;
	
	// private static boolean generated = false;
	
	@Override
	public boolean start() {
		lock();
		if (running.future().isComplete())
			return true;

		VertxOptions options = new VertxOptions();
		web = Vertx.vertx(options);
		web.deployVerticle(new ServerVerticle(this));
		return true;
	}
	
	@Override
	public boolean stop() {
		if (running.future().isComplete()) {
			web.close();
			running = Promise.promise();
			return true;
		}
		return false;
	}
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		cep = (CentralEntityProxy) getAgent();
	}

	/**
	 * Sends a message to the client after a connection has been established.
	 * @param message - the message to be sent.
	 */
	protected void sendToClient(String message) {
		running.future().onComplete(asyncResult -> {
			if (asyncResult.succeeded()) {
				web.eventBus().send(SERVER_TO_CLIENT, message);
			}
		});
	}

	/**
	 * Constructs a message for the client.
	 * @param scope - the scope of the message.
	 * @param subject - the subject of the message.
	 * @param content - the content of the message.
	 * @return the message as a string.
	 */
	protected static String buildMessage(String scope, String subject, JsonObject content) {
		JsonObject message = new JsonObject();
		message.addProperty(MESSAGE_SCOPE, scope);
		message.addProperty(MESSAGE_SUBJECT, subject);
		message.add(MESSAGE_CONTENT, content);
		return message.toString();
	}

	/**
	 * Constructs a message for the client.
	 * @param scope - the scope of the message.
	 * @param subject - the subject of the message.
	 * @param content - the content of the message.
	 * @return the message as a string.
	 */
	protected static String buildMessage(String scope, JsonObject subject, JsonObject content) {
		JsonObject message = new JsonObject();
		message.addProperty(MESSAGE_SCOPE, scope);
		message.add(MESSAGE_SUBJECT, subject);
		message.add(MESSAGE_CONTENT, content);
		return message.toString();
	}
	
	/**
	 * @return a {@link JsonObject} containing the specifications of all entities.
	 */
	public JsonObject getEntities() {
		JsonObject specifications = new JsonObject();
		lf("All entity GUIs: []", entityGUIs);
		entityGUIs.entrySet().forEach(entry -> {
			String entityName = entry.getKey();
			Element guiSpecification = entry.getValue();

			lf("Entity GUI: [], []", entityName, guiSpecification);
			specifications.add(entityName, guiSpecification.toJSON());
		});
		return specifications;
	}
	
	@Override
	public boolean updateGui(String entity, Element guiSpecification) {
		// Andrei Olaru: placed this here as a workaround, don't know why entity is null
		if (entity == null)
			return false;
		idManager.removeIdsWithPrefix(entity);
		idManager.insertIdsInto(guiSpecification, entity);
		
		super.updateGui(entity, guiSpecification);
		
		String message = buildMessage("entity", "update", 
			guiSpecification.toJSON());
		sendToClient(message);
		return true;
	}
	
	/**
	 * Sends an event to the agent when an active input on a port is received from the client.
	 * @param subject - a JsonArray containing the destination elements on the agent
	 * @param content - a JsonObject containing all the roles and their values
	 */
	public void sendNotification(JsonArray subject, JsonObject content) {
		// Special actions:
		String port = subject.get(1).getAsString();
		String role = subject.get(2).getAsString();
		if ("standard-stop".equals(port) && "stop".equals(role)) {
			sendEvent(AgentEventType.AGENT_STOP);
			return;
		}
		if ("standard-start".equals(port) && "start".equals(role)) {
			sendEvent(AgentEventType.AGENT_START);
			return;
		}
		// Normal notification:
		AgentWave wave = new AgentWave(
			content.toString(),
			CentralMonitoringAndControlEntity.Operations.GUI_INPUT_TO_ENTITY.toString(),
			gson.fromJson(subject, String[].class)
		);
			
		wave.addSourceElements(getShardDesignation().toString());
		li("Sending notification to client: []", wave.toString());
		cep.postAgentEvent(wave);
	}

	/**
	 * Sends an event to the agent (for e.g. start/stop)
	 * @param eventType - the type of the event to be sent.
	 */
	public void sendEvent(AgentEventType eventType) {
		li("Sending event to agent: []", eventType);
		AgentEvent event = new AgentEvent(eventType);
		cep.postAgentEvent(event);
	}
	
	@Override
	public boolean sendOutput(AgentWave wave) {
		super.sendOutput(wave);
		li("Sending output wave: []", wave.toString());
		
		// wave destination is entity/port
		String entity = wave.popDestinationElement();
		String port = wave.popDestinationElement();
		JsonObject subject = new JsonObject();
		subject.addProperty("entity", entity);
		subject.addProperty("port", port);
		
		Element gui = entityGUIs.get(entity);
		if (gui == null)
			return ler(false, "GUI for entity [] not present.", entity);
		
		JsonObject allRoles = new JsonObject();
		for (String role : wave.getContentElements()) {
			allRoles.addProperty(role, wave.getValue(role));
		}
		
		String message = buildMessage(PORT_SCOPE, subject, allRoles);
		sendToClient(message);
		return true;
	}
}

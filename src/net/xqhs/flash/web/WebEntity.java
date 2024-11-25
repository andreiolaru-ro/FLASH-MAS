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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.monitoring.CentralGUI;
import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity;
import net.xqhs.flash.core.monitoring.CentralMonitoringAndControlEntity.CentralEntityProxy;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.OperationUtils.MonitoringOperation;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.gui.structure.ElementIdManager;

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

	/** Address for ws communication from server to client agent */
	protected static final String SERVER_TO_CLIENT_AGENT = "server-to-client-agent";

	/** Address for ws communication from client to server agent */
	protected static final String CLIENT_TO_SERVER_AGENT = "client-to-server-agent";

	/** The endpoint for ws communication */
	protected static final String WS_ENDPOINT = "/eventbus";

	/**
	 * The server verticle for the web entity.
	 */
	class ServerVerticle extends AbstractVerticle {
		/**
		 * The web entity this verticle is associated with.
		 */
		private WebEntity entity;

		/**
		 * The port on which the server is running.
		 */
		protected int port = 8081;

		/**
		 * The number of connections to the server.
		 */
		protected int numConnections = 0;

		/**
		 * Constructor for the server verticle.
		 * 
		 * @param entity
		 *               - the web entity this verticle is associated with.
		 */
		public ServerVerticle(WebEntity entity) {
			this.entity = entity;
		}

		@Override
		public void start(Future<Void> startFuture) throws Exception {
			Router router = Router.router(vertx);
			SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
			BridgeOptions options = new BridgeOptions()
					.addOutboundPermitted(new PermittedOptions().setAddress(SERVER_TO_CLIENT))
					.addOutboundPermitted(new PermittedOptions().setAddress(SERVER_TO_CLIENT_AGENT))
					.addInboundPermitted(new PermittedOptions().setAddress(CLIENT_TO_SERVER))
					.addInboundPermitted(new PermittedOptions().setAddress(CLIENT_TO_SERVER_AGENT));

			// mount the bridge on the router
			router.mountSubRouter(WS_ENDPOINT, sockJSHandler.bridge(options, bridgeEvent -> {
				try {
					if (bridgeEvent.type() == BridgeEventType.SOCKET_CREATED) {
						System.out.println("created");
					} else if (bridgeEvent.type() == BridgeEventType.REGISTER) {
						System.out.println("register");
						numConnections++;

						String entities = getEntities().toString();
						vertx.eventBus().send(SERVER_TO_CLIENT, entities);
					} else if (bridgeEvent.type() == BridgeEventType.UNREGISTER) {
						numConnections--;
						System.out.println("unregister");
					} else if (bridgeEvent.type() == BridgeEventType.SOCKET_CLOSED) {
						numConnections--;
						System.out.println("closed");
					} else if (bridgeEvent.type() == BridgeEventType.SEND) {
						System.out.println("client-message");
					} else if (bridgeEvent.type() == BridgeEventType.RECEIVE) {
						System.out.println("server-message");
					}
				} catch (Exception e) {
					// do nothing
				} finally {
					bridgeEvent.complete(Boolean.valueOf(true));
				}
			}));

			vertx.eventBus().consumer(CLIENT_TO_SERVER).handler(objectMessage -> {
				JsonObject msg = JsonParser.parseString((String) objectMessage.body()).getAsJsonObject();
				if ("port".equals(msg.get("scope").getAsString()))
					activeInput(msg);
			});

			System.out.println(WebEntity.class.getPackage().getName());
			String pkg = DeploymentConfiguration.SOURCE_FILE_DIRECTORIES[0] + "/"
					+ WebEntity.class.getPackage().getName().replace(".", "/");

			router.route().handler(
				StaticHandler.create(pkg).setIndexPage("page.html").setCachingEnabled(false)
			);
			
			vertx.createHttpServer().requestHandler(router).listen(port, http -> {
				if (http.succeeded())
					li("HTTP server started on port []", port);
				else
					li("HTTP server failed to start on port []", port);
			});
		}

		@Override
		public void stop(Future<Void> stopFuture) throws Exception {
			System.out.println("HTTP server stoped");
		}
	}

	static CentralEntityProxy cep;

	// public static JsonObject agentMessages;

	private Element specification;

	protected ElementIdManager idManager = new ElementIdManager();

	private Vertx web;

	private boolean running = false;

	// protected boolean verticleReady = false;

	// private static boolean generated = false;

	@Override
	public boolean start() {
		lock();
		if (!running) {
			VertxOptions options = new VertxOptions();
			web = Vertx.vertx(options);
			web.deployVerticle(new ServerVerticle(this));
			running = true;
		}
		return running;
	}

	@Override
	public boolean stop() {
		if (running) {
			web.close();
			running = false;
		}
		return running;
	}

	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		cep = (CentralEntityProxy) getAgent();
	}

	/**
	 * @return a {@link JsonObject} containing the specifications of all entities.
	 */
	public JsonObject getEntities() {
		System.out.println("entities get.");

		JsonObject specifications = new JsonObject();
		JsonObject types = new JsonObject();
		JsonObject activators = new JsonObject();

		entityGUIs.entrySet().forEach(entry -> {
			String name = entry.getKey();
			Element guiElement = entry.getValue();

			specifications.add(name, guiElement.toJSON());
			for (Element child : guiElement.getChildren()) {
				types.addProperty(child.getId(), child.getType());
				if ("activate".equals(child.getRole())) {
					JsonArray role_ids = new JsonArray();
					for (Element port_e : guiElement.getChildren(child.getPort()))
						role_ids.add(port_e.getId());
					activators.add(child.getId(), role_ids);
				}
			}
		});
		JsonObject result = new JsonObject();
		result.addProperty("scope", "global");
		result.addProperty("subject", "entities list");
		JsonObject content = new JsonObject();
		content.add("specification", specifications);
		content.add("types", types);
		content.add("activators", activators);
		result.add("content", content);
		System.out.println("entities get: " + specifications.toString());
		return result;
	}

	@Override
	public boolean updateGui(String entity, Element guiSpecification) {
		super.updateGui(entity, guiSpecification);

		// Andrei Olaru: placed this here as a workaround, don't know why entity is null
		if (entity == null)
			return false;
		idManager.removeIdsWithPrefix(entity);
		idManager.insertIdsInto(guiSpecification, entity);

		JsonObject tosend = new JsonObject();
		tosend.addProperty("scope", "entity");
		tosend.addProperty("subject", "update");
		tosend.add("content", guiSpecification.toJSON());
		web.eventBus().send(SERVER_TO_CLIENT, tosend.toString());
		return true;
	}

	/**
	 * Posts an {@link AgentWave} to the {@link CentralEntityProxy} when an active
	 * input arrives from the web client.
	 * <ul>
	 * <li>The source is the shard designation (gui).
	 * <li>The destination is {@link MonitoringOperation#GUI_INPUT_TO_ENTITY} /
	 * entity / gui / port
	 * <li>The wave contains all the values for each role in the specification.
	 * </ul>
	 * 
	 * @param msg
	 *            - the data received from the web client.
	 */
	protected void activeInput(JsonObject msg) {
		AgentWave wave = new AgentWave(null, MonitoringOperation.GUI_INPUT_TO_ENTITY.getOperation());
		wave.addSourceElements(getShardDesignation().toString());
		Element activatedElement = idManager.getElement(msg.get("subject").getAsString());
		if (activatedElement == null) {
			le("Element for id [] not found.", msg.get("subject").getAsString());
			return;
		}
		String entityName = idManager.getEntity(msg.get("subject").getAsString());
		if (entityName == null) {
			le("Entity for id [] not found.", msg.get("subject").getAsString());
			return;
		}
		String port = activatedElement.getPort();
		wave.appendDestination(entityName, StandardAgentShard.GUI.shardName(), port);
		JsonObject content = new JsonObject();
		if (msg.get("subject").getAsString().split("_")[1]
				.contains(CentralMonitoringAndControlEntity.getNodeOperationsPrefix())) {
			content.addProperty(OperationUtils.PARAMETERS, entityName);
			content.addProperty(OperationUtils.OPERATION_NAME, msg.get("content").getAsJsonObject()
					.get(msg.get("content").getAsJsonObject().keySet().toArray()[0].toString())
					.getAsString().toLowerCase());
			wave.add("content", content.toString());
		} else {
			Element entityElement = entityGUIs.get(entityName);
			content = msg.get("content").getAsJsonObject();
			for (Element element : entityElement.getChildren(port)) {
				if (content.has(element.getId()))
					wave.add(element.getRole(), content.get(element.getId()).getAsString());
			}
		}
		cep.postAgentEvent(wave);
	}

	@Override
	public void sendOutput(AgentWave wave) {
		JsonObject tosend = new JsonObject();
		tosend.addProperty("scope", "port");

		String entity = wave.popDestinationElement();
		String port = wave.getFirstDestinationElement();
		tosend.addProperty("subject", idManager.makeID(null, entity, port)); // questionable abuse of makeID

		Element gui = entityGUIs.get(entity);
		if (gui == null) {
			le("GUI for entity [] not present.", entity);
			return;
		}
		JsonObject allValues = new JsonObject();
		for (String role : wave.getContentElements()) {
			int i = 0;
			for (Element e : gui.getChildren(port, role))
				allValues.addProperty(e.getId(), wave.getValues(role).get(i++));
		}
		tosend.add("content", allValues);
		web.eventBus().send(SERVER_TO_CLIENT, tosend.toString());
	}
}

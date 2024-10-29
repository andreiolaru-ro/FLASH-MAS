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
package web;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
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
import net.xqhs.flash.gui.structure.GlobalConfiguration;

/**
 * Web entity for the connection between the MAS and the web interface.
 */
public class WebEntity extends CentralGUI {
	
	class ServerVerticle extends AbstractVerticle {
		private WebEntity entity;
		
		private boolean handler = false;
		
		protected int port = 8081;
		
		public ServerVerticle(WebEntity entity) {
			this.entity = entity;
		}
		
		@Override
		public void start(Future<Void> startFuture) throws Exception {
			Router router = Router.router(vertx);
			SockJSHandler sockJSHandler = SockJSHandler.create(vertx);
			BridgeOptions options = new BridgeOptions()
					.addOutboundPermitted(new PermittedOptions().setAddress("server-to-client"))
					.addInboundPermitted(new PermittedOptions().setAddress("client-to-server"))
					.addOutboundPermitted(new PermittedOptions().setAddress("server-to-client-agent-message"))
					.addInboundPermitted(new PermittedOptions().setAddress("client-to-server-agent-message"));
			// mount the bridge on the router
			router.mountSubRouter("/eventbus", sockJSHandler.bridge(options, be -> {
				try {
					if(be.type() == BridgeEventType.SOCKET_CREATED) {
						System.out.println("created");
					}
					else if(be.type() == BridgeEventType.REGISTER) {
						System.out.println("register");
						verticleReady = true;
						String tosend = getEntities().toString();
						vertx.eventBus().send("server-to-client", tosend);
						
						if(!handler)
							vertx.eventBus().consumer("client-to-server").handler(objectMessage -> {
								JsonObject msg = new JsonObject((String) objectMessage.body());
								if("port".equals(msg.getString("scope")))
									activeInput(msg);
							});
							
						// else {
						// System.out.println(objectMessage.body());
						// CentralEntityProxy cep = entity.cep;
						// JsonObject data = new JsonObject((String) objectMessage.body());
						// Iterator<Map.Entry<String, Object>> entryIterator = data.iterator();
						//
						// while(entryIterator.hasNext()) {
						// Map.Entry<String, Object> entry = entryIterator.next();
						//
						// String entity = entry.getKey();
						// JsonObject input = (JsonObject) entry.getValue();
						// if(input.getString("type").equals("operation")) {
						// // TODO: opertaions for entities do not have parameters yet
						// String name = input.getString("name").split(" ")[1];
						// String[] parameters = input.getString("name").split(" ");
						// if(entity.equals("all"))
						// cep.sendToAllAgents(name);
						// else
						// cep.sendToEntity(entity, name);
						// }
						// else if(input.getString("type").equals("message")) {
						// String[] content_destination = input.getString("content_destination")
						// .split(" ");
						// int n = content_destination.length - 1;
						// JsonObject message = new JsonObject();
						// String destination = content_destination[n];
						// message.put("agent", entity);
						// message.put("content", content_destination[1]);
						// for(int m = 2; m < n; m++)
						// message.put("content",
						// message.getString("content") + " " + content_destination[m]);
						// cep.sendAgentMessage(destination, message.toString());
						// }
						// else {
						// // TODO: needed for other input options
						// }
						// }
						// }
						// });
						// vertx.setPeriodic(10000l, t -> {
						// JsonObject entities = new JsonObject((String) WebEntity.cep.getEntities());
						// vertx.eventBus().send("server-to-client", entities.toString());
						// });
						// handler = true;
						// }
						// else {
						// vertx.eventBus().consumer("client-to-server-agent-message").handler(objectMessage -> {
						// System.out.println("Message seen for agent " + objectMessage.body());
						// });
						// vertx.setPeriodic(1000l, t -> {
						// // vertx.eventBus().send("server-to-client-agent-message",
						// // WebEntity.agentMessages.toString());
						// });
						// }
					}
					else if(be.type() == BridgeEventType.UNREGISTER) {
						verticleReady = false;
						System.out.println("unregister");
					}
					else if(be.type() == BridgeEventType.SOCKET_CLOSED) {
						verticleReady = false;
						System.out.println("closed");
					}
					else if(be.type() == BridgeEventType.SEND) {
						System.out.println("client-message");
					}
					else if(be.type() == BridgeEventType.RECEIVE) {
						System.out.println("server-message");
						
					}
				} catch(Exception e) {
					
				} finally {
					be.complete(true);
				}
			}));
			router.route().handler(StaticHandler.create("src/web").setIndexPage("page.html"));
			vertx.createHttpServer().requestHandler(router).listen(port, http -> {
				if(http.succeeded())
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
	
	private boolean verticleReady = false;
	
	// private static boolean generated = false;
	
	public WebEntity(GlobalConfiguration config) {
		// if(!generated) {
		// PageBuilder.getInstance().platformType = PlatformType.WEB;
		// try {
		// agentMessages = new JsonObject();
		//
		// BuildPageTest.main(new String[] {"file", "interface-files/model-page/web-page.yml"});
		// specification = PageBuilder.getInstance().getPage();
		//
		// generated = true;
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// }
		// start();
	}
	
	@Override
	public boolean start() {
		lock();
		if(!running) {
			VertxOptions options = new VertxOptions();
			web = Vertx.vertx(options);
			web.deployVerticle(new ServerVerticle(this));
			running = true;
		}
		return running;
	}
	
	@Override
	public boolean stop() {
		if(running) {
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
	 * @return a {@link JSONObject} containing the specifications of all entities.
	 */
	public JSONObject getEntities() {
		System.out.println("entities get.");
		JSONObject specifications = new JSONObject();
		JSONObject types = new JSONObject();
		JSONObject activators = new JSONObject();
		entityGUIs.keySet().forEach(name -> {
			specifications.put(name, entityGUIs.get(name).toJSON());
			for(Element e : entityGUIs.get(name).getChildren()) {
				types.put(e.getId(), e.getType());
				if("activate".equals(e.getRole())) {
					JSONArray role_ids = new JSONArray();
					for(Element port_e : entityGUIs.get(name).getChildren(e.getPort()))
						role_ids.add(port_e.getId());
					activators.put(e.getId(), role_ids);
				}
			}
		});
		JSONObject result = new JSONObject();
		result.put("scope", "global");
		result.put("subject", "entities list");
		JSONObject content = new JSONObject();
		content.put("specification", specifications);
		content.put("types", types);
		content.put("activators", activators);
		result.put("content", content);
		System.out.println("entities get: " + specifications.toString());
		return result;
	}
	
	@Override
	public boolean updateGui(String entity, Element guiSpecification) {
		super.updateGui(entity, guiSpecification);
		
		// Andrei Olaru: placed this here as a workaround, don't know why entity is null
		if(entity == null)
			return false;
		idManager.removeIdsWithPrefix(entity);
		idManager.insertIdsInto(guiSpecification, entity);
		
		JSONObject tosend = new JSONObject();
		tosend.put("scope", "entity");
		tosend.put("subject", "update");
		tosend.put("content", guiSpecification.toJSON());
		web.eventBus().send("server-to-client", tosend.toString());
		return true;
	}
	
	/**
	 * Posts an {@link AgentWave} to the {@link CentralEntityProxy} when an active input arrives from the web client.
	 * <ul>
	 * <li>The source is the shard designation (gui).
	 * <li>The destination is {@link MonitoringOperation#GUI_INPUT_TO_ENTITY} / entity / gui / port
	 * <li>The wave contains all the values for each role in the specification.
	 * </ul>
	 * 
	 * @param msg
	 *            - the data received from the web client.
	 */
	protected void activeInput(JsonObject msg) {
		AgentWave wave = new AgentWave(null, MonitoringOperation.GUI_INPUT_TO_ENTITY.getOperation());
		wave.addSourceElements(getShardDesignation().toString());
		Element activatedElement = idManager.getElement(msg.getString("subject"));
		if(activatedElement == null) {
			le("Element for id [] not found.", msg.getString("subject"));
			return;
		}
		String entityName = idManager.getEntity(msg.getString("subject"));
		if(entityName == null) {
			le("Entity for id [] not found.", msg.getString("subject"));
			return;
		}
		String port = activatedElement.getPort();
		wave.appendDestination(entityName, StandardAgentShard.GUI.shardName(), port);
		JsonObject content = new JsonObject();
		if (msg.getString("subject").split("_")[1].contains(CentralMonitoringAndControlEntity.getNodeOperationsPrefix())) {
			content.put(OperationUtils.PARAMETERS, entityName);
			content.put(OperationUtils.OPERATION_NAME, msg.getJsonObject("content")
					.getString(msg.getJsonObject("content").fieldNames().toArray()[0].toString()).toLowerCase());
			wave.add("content", content.toString());
		}
		else {
			Element entityElement = entityGUIs.get(entityName);
			content = msg.getJsonObject("content");
			for(Element element : entityElement.getChildren(port)) {
				if(content.containsKey(element.getId()))
					wave.add(element.getRole(), content.getString(element.getId()));
			}
		}
		cep.postAgentEvent(wave);
	}

	@Override
	public void sendOutput(AgentWave wave) {
		JSONObject tosend = new JSONObject();
		tosend.put("scope", "port");
		
		String entity = wave.popDestinationElement();
		String port = wave.getFirstDestinationElement();
		tosend.put("subject", idManager.makeID(null, entity, port)); // questionable abuse of makeID
		
		Element gui = entityGUIs.get(entity);
		if(gui == null) {
			le("GUI for entity [] not present.", entity);
			return;
		}
		JSONObject allValues = new JSONObject();
		for(String role : wave.getContentElements()) {
			int i = 0;
			for(Element e : gui.getChildren(port, role))
				allValues.put(e.getId(), wave.getValues(role).get(i++));
		}
		tosend.put("content", allValues);
		web.eventBus().send("server-to-client", tosend.toString());
	}
}

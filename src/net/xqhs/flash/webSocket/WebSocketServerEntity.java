package net.xqhs.flash.webSocket;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.Entity;

/**
 *  The {@link WebSocketServerEntity} class manages the routing of messages between different entities. It knows the
 *  available agents in the FLASH-MAS system along with their websocket address. It also keeps track of central nodes.
 *
 *  @author Florina Nastasoiu
 */
public class WebSocketServerEntity extends Unit implements Entity
{
	{
		setUnitName("websocket-server").setLoggerType(PlatformUtils.platformLogType());
	}
	
	private static final int		SERVER_STOP_TIME	= 10;
	private WebSocketServer			webSocketServer;
	private boolean					running				= false;

	private HashMap<String, WebSocket> entityToWebSocket = new HashMap<>();
	private HashMap<String, WebSocket> nodeToWebSocket  = new HashMap<>();
	private HashMap<String, List<String>> nodeToEntities = new LinkedHashMap<>();

	private boolean sendFurther(JSONObject jsonObject) {
		// raw message from one entity to another
			String destination = (String) jsonObject.get("destination");
			String destEntity = destination.split(
					AgentWave.ADDRESS_SEPARATOR)[0];

			WebSocket destinationWebSocket = entityToWebSocket.get(destEntity);
			if(destinationWebSocket != null) {
				destinationWebSocket.send(jsonObject.toString());
				li("Sent to agent: []. ", jsonObject.toString());
				return true;
			}

			destinationWebSocket = nodeToWebSocket.get(destEntity);
			if(destinationWebSocket != null) {
				destinationWebSocket.send(jsonObject.toString());
				li("Sent to node: []. ", jsonObject.toString());
				return true;
			}

			le("Filed to find the entity [] websocket.", destEntity);
			return false;
	}


	public WebSocketServerEntity(int serverAddress)
	{
		webSocketServer = new WebSocketServer(new InetSocketAddress(serverAddress)) {
			
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
			{
				/*
				 * This method sends a message to the new client.
				 */
				li("new client connected []", webSocket);
			}
			
			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b)
			{
				li(("[] closed with exit code " + i), webSocket);
			}

			/**
			 * Receives message from a {@link WebSocketClient}.
			 * Messages can be:
			 * 					- node registration message
			 * 					- agent registration message
			 * 					- central entity for monitoring and control message
			 * 					- raw message from one entity to another
			 *
			 * @param webSocket
			 * 					- the sender websocket client
			 * @param s
			 * 					- the JSON string containing a message and routing information
			 */
			@Override
			public void onMessage(WebSocket webSocket, String s)
			{
				Object obj = JSONValue.parse(s);
				if(obj == null) return;
				JSONObject jsonObject = (JSONObject) obj;

				// message in transit through the server
				if(jsonObject.get("destination") != null && sendFurther(jsonObject))
					return;

				// specify if the entity will be registered or unregistered
				boolean toRegister = false;
				if(jsonObject.get("register") != null)
					toRegister = (boolean)jsonObject.get("register");

				if(jsonObject.get("nodeName") == null) return;
				String nodeName = (String)jsonObject.get("nodeName");

				// node registration message
                if(jsonObject.size() == 1)
				{
					nodeToWebSocket.put(nodeName, webSocket);
					nodeToEntities.put(nodeName, new ArrayList<>());
					li("Registered node []. ", nodeName);
					printState();
					return;
				}

				// agent registration message
				String newEntity;
				if(jsonObject.get("entityName") != null)
				{
					newEntity = (String)jsonObject.get("entityName");
					if(toRegister) {
						entityToWebSocket.put(newEntity, webSocket);
						nodeToEntities.get(nodeName).add(newEntity);
						li("Registered entity []. ", newEntity);
					} else {
						entityToWebSocket.remove(newEntity);
						nodeToEntities.get(nodeName).remove(newEntity);
						li("Unregistered entity []. ", newEntity);
					}
					printState();
					return;
				}
			}
			
			@Override
			public void onError(WebSocket webSocket, Exception e)
			{
				e.printStackTrace();
			}
			
			@Override
			public void onStart()
			{
				li("Server started successfully.");
			}

			private void printState() {
				li("###agent:  " + entityToWebSocket.keySet());
				li("###nodes: " + nodeToEntities.keySet());
				li("###agents: " + nodeToEntities.values());
			}
		};
	}
	
	@Override
	public boolean start()
	{
		webSocketServer.start();
		running = true;
		return true;
	}

	/**
	 * @return <code>true</code> if the operation was successful. <code>false</code> otherwise.
	 */
	@Override
	public boolean stop()
	{
		try
		{
			webSocketServer.stop(SERVER_STOP_TIME);
			running = false;
			return true;
		} catch(InterruptedException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	@Override
	public boolean isRunning()
	{
		return running;
	}
	
	@Override
	public String getName()
	{
		return null;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean addContext(EntityProxy context)
	{
		return false;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean removeContext(EntityProxy context)
	{
		return false;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean removeGeneralContext(EntityProxy context)
	{
		return false;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public EntityProxy asContext()
	{
		return null;
	}

	/**
	 * Functionality not used yet.
	 */
	@Override
	public boolean addGeneralContext(EntityProxy context)
	{
		return false;
	}
}

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
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.Entity;

public class WebSocketServerEntity extends Unit implements Entity
{
	{
		setUnitName("websocket-server").setLoggerType(PlatformUtils.platformLogType());
	}
	
	private static final int		SERVER_STOP_TIME	= 10;
	private WebSocketServer			webSocketServer;
	private boolean					running				= false;

	private HashMap<String, WebSocket> agentToWebSocket = new HashMap<>();
	private HashMap<String, WebSocket> nodeToWebSocket  = new HashMap<>();
	private HashMap<String, List<String>> nodeToAgents = new LinkedHashMap<>();

	private String centralNodeName;
	private WebSocket centralNodeWebSocket;


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
			
			@Override
			public void onMessage(WebSocket webSocket, String s)
			{
				Object obj = JSONValue.parse(s);
				if(obj == null) return;
				JSONObject jsonObject = (JSONObject) obj;

				if(jsonObject.get("nodeName") == null) return;
				String nodeName = (String)jsonObject.get("nodeName");

				boolean isCentralNode;
				if(jsonObject.get("isCentral") != null)
				{
					isCentralNode = (boolean)jsonObject.get("isCentral");
					if(isCentralNode)
					{
						centralNodeName = nodeName;
						centralNodeWebSocket = webSocket;
					}
					nodeToWebSocket.put(nodeName, webSocket);
					nodeToAgents.put(nodeName, new ArrayList<>());
					li("Registered node []. ", nodeName);
					return;
				}

				String newAgent;
				if(jsonObject.get("agentName") != null)
				{
					newAgent = (String)jsonObject.get("agentName");
					agentToWebSocket.put(newAgent, webSocket);
					nodeToAgents.get(nodeName).add(newAgent);
					li("Registered agent []. ", newAgent);
					return;
				}

				if(jsonObject.get("destination") != null) {
					li("Received: []. ", s);
					String destination = (String) jsonObject.get("destination");
					String destAgent = destination.split(
							AgentWave.ADDRESS_SEPARATOR)[0];
					WebSocket destinationWebSocket = agentToWebSocket.get(destAgent);
					if(destinationWebSocket == null) {
						le("Filed to find the entity [] websocket.", destAgent);
						return;
					}
					destinationWebSocket.send(s);
				}

				nodeToWebSocket.entrySet().forEach(entry->{
					System.out.println("#nod: " + entry.getKey());
				});

				agentToWebSocket.entrySet().forEach(entry->{
					System.out.println("#agent: " + entry.getKey());
				});

				nodeToAgents.entrySet().forEach(entry->{
					System.out.println("#nod->ent: " + entry.getKey() + " : " + entry.getValue());
				});

				System.out.println("##CentralNode " + centralNodeName);
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
		};
	}
	
	@Override
	public boolean start()
	{
		webSocketServer.start();
		running = true;
		return true;
	}
	
	@Override
	public boolean stop()
	{
		running = false;
		try
		{
			webSocketServer.stop(SERVER_STOP_TIME);
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
	
	@Override
	public boolean addContext(EntityProxy context)
	{
		return false;
	}
	
	@Override
	public boolean removeContext(EntityProxy context)
	{
		return false;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy context)
	{
		return false;
	}
	
	@Override
	public EntityProxy asContext()
	{
		return null;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy context)
	{
		return false;
	}
}

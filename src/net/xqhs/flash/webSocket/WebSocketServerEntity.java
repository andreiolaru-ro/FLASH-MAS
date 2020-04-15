package net.xqhs.flash.webSocket;

import java.net.InetSocketAddress;
import java.util.HashMap;

import net.xqhs.flash.core.agent.AgentWave;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.Entity;

public class WebSocketServerEntity implements Entity
{
	
	protected static final int			SERVER_STOP_TIME	= 10;
	protected WebSocketServer			webSocketServer;
	protected boolean					running				= false;
	/*
	 * All clients' addresses are stored in case one of them is target socket.
	 */
	private HashMap<String, WebSocket>	nameConnections		= new HashMap<String, WebSocket>();
	
	public WebSocketServerEntity(int serverAddress)
	{
		webSocketServer = new WebSocketServer(new InetSocketAddress(serverAddress)) {
			
			@Override
			public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake)
			{
				/*
				 * This method sends a message to the new client.
				 */
				webSocket.send("[Server] : Welcome to the Server!");
			}
			
			@Override
			public void onClose(WebSocket webSocket, int i, String s, boolean b)
			{
				broadcast("[Server]" + webSocket + " has left the room");
			}
			
			@Override
			public void onMessage(WebSocket webSocket, String s)
			{
				/*
				 * The name of a new agent was send and we need to store it.
				 */
				String[] namePayload = s.split("=");
				if(namePayload.length == 2)
					nameConnections.put(namePayload[1], webSocket);
				else
				{
					/*
					 * A JSON with source, destination and content is received.
					 */
					Object obj = JSONValue.parse(s);
					if(obj == null) return;

					JSONObject jsonObject = (JSONObject) obj;
					if(jsonObject.get("destination") == null) return;

					String destination = (String) jsonObject.get("destination");
					String destAgent = destination.split(
							AgentWave.ADDRESS_SEPARATOR)[0];
					WebSocket destinationWebSocket = nameConnections.get(destAgent);
					if(destinationWebSocket == null) return;

					destinationWebSocket.send(s);
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
				System.out.println(("ServerPylon started successfully!"));
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

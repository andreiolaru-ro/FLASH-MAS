package net.xqhs.flash.webSocket;

import java.net.InetSocketAddress;
import java.util.HashMap;

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

				if(jsonObject.get("name") != null)
				{
					String name = (String) jsonObject.get("name");
					nameConnections.put(name, webSocket);
					li("Registered agent []. ", name);
					return;
				}
				if(jsonObject.get("destination") == null) return;

				li("Received: []. ", s);
				String destination = (String) jsonObject.get("destination");
				String destAgent = destination.split(
						AgentWave.ADDRESS_SEPARATOR)[0];
				WebSocket destinationWebSocket = nameConnections.get(destAgent);
				if(destinationWebSocket == null) return;

				destinationWebSocket.send(s);
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

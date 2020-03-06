package websockets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashMap;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class WebSocketServerEntity extends WebSocketServer
{
    /**
     * Keep all incoming connected clients for future connections.
     */
    private HashMap<String, WebSocket> nameConnections = new HashMap<String, WebSocket>();

    public WebSocketServerEntity(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        /*
         * This method sends a message to the new client.
         */
        webSocket.send("[ ServerPylon ] : Welcome to the ServerPylon!");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        broadcast("[ ServerPylon ]" + webSocket +  " has left the room");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        /* The name of a new agent was send. */
        String[] namePayload = s.split("=");
        if (namePayload.length == 2)
            nameConnections.put(namePayload[1], webSocket);
        else {
            Object obj = JSONValue.parse(s);
            JSONObject jsonObject = (JSONObject) obj;

            String destination = (String) jsonObject.get("destination");
            WebSocket destinationWebSocket = nameConnections.get(destination);
            destinationWebSocket.send(s);
        }
    }

    @Override
    public void onMessage(WebSocket websocket, ByteBuffer message) {
        broadcast(message.array());
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        e.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println(("ServerPylon started successfully!"));
    }

    public static void main(String[] args) throws IOException {
        int pylonPort = 8886;
        WebSocketServer pylonServer = new WebSocketServerEntity(new InetSocketAddress(pylonPort));
        pylonServer.run();

        BufferedReader system_in = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            String input;
            input = system_in.readLine();
            if(input.equals(("exit"))) {
                try {
                    pylonServer.stop(10);
                    break;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}

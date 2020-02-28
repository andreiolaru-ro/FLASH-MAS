package websocketsTest;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.HashMap;

public class ServerPylon extends WebSocketServer {
    private HashMap<String, WebSocket> nameConnections = new HashMap<String, WebSocket>();

    public ServerPylon(InetSocketAddress address) {
        super(address);
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        /*
         * This method sends a message to the new client.
         * TODO: Check how the client is found in the route table in this framework.
         */
        webSocket.send("[from server] : Welcome to the ServerPylon!");
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        broadcast(webSocket +  " has left the room");
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        String[] namePayload = s.split("=");
        if (namePayload.length == 2)
            nameConnections.put(namePayload[1], webSocket);
        else
        {
            String[] messagePayload = s.split("/");
            if(messagePayload.length != 0) {
                String destination = messagePayload[1];
                String message = messagePayload[2];
                WebSocket destinationWebSocket = nameConnections.get(destination);
                destinationWebSocket.send(message);
            }
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
        String pylonHost = "localhost";
        int pylonPort = 8883;
        WebSocketServer pylonServer = new ServerPylon(new InetSocketAddress(pylonPort));
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

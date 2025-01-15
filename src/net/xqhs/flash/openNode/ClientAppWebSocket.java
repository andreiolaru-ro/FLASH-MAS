package net.xqhs.flash.openNode;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.Scanner;

public class ClientAppWebSocket {
    private static WebSocketClient webSocketClient;

    public static void main(String[] args) {
        try{
            // Config WebSocket client
            URI serverUri = new URI("ws://localhost:8885");
            webSocketClient = new WebSocketClient(serverUri){
                @Override
                public void onOpen(ServerHandshake handshake){
                    System.out.println("Connected to WebSocket server");
                }
                @Override
                public void onMessage(String message){
                    System.out.println("Message from server: " + message);
                }
                @Override
                public void onClose(int code, String reason, boolean remote){
                    System.out.println("Closed with exit code " + code + ", additional info: " + reason);
                }
                @Override
                public void onError(Exception ex) {
                    ex.printStackTrace();
                }
            };
            webSocketClient.connect();

            Scanner scanner = new Scanner(System.in);
            while(true){
                System.out.println("Enter command: ");
                String command = scanner.nextLine().trim();
                System.out.println("Command entered: " + command);

                if(command.equalsIgnoreCase("exit")){
                    break;
                } else if (command.startsWith("add -agent")){
                    String[] parts = command.split(" ", 5);
                    if (parts.length == 5 && parts[3].equalsIgnoreCase("-shard")){
                        String agentName = parts[2];
                        String shardName = parts[4];

                        String message = "addAgent " + agentName + " " + shardName;
                        webSocketClient.send(message);
                        System.out.println("Sent addAgent message: " + message);
                    } else {
                        System.out.println("Invalid command format. Usage: add -agent AgentName -shard ShardName");
                    }
                } else if (command.equalsIgnoreCase("list agents")) {
                    webSocketClient.send("listAgents");
                    System.out.println("Requesting list of agents from server...");
                } else {
                    System.out.println("Unknown command. Valid commands: add -agent AgentName -shard ShardName, exit");
                }
            }
            scanner.close();
            webSocketClient.close();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
    }
}
}

package net.xqhs.flash.core.node.clientApp;

import net.xqhs.flash.rmi.NodeCLI;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;
import java.util.Scanner;

public class ClientApp extends UnicastRemoteObject implements ClientCallbackInterface, Serializable , Remote {
    private static ClientApp instance;

    protected ClientApp() throws RemoteException {
        super();
    }

    public static synchronized ClientApp getInstance() throws RemoteException {
        if (instance == null) {
            instance = new ClientApp();
        }
        return instance;
    }

    @Override
    public void notifyAgentAdded(String agentName) throws RemoteException {
        System.out.println("Notification: Agent " + agentName + " has been added.");
    }

    public static void main(String[] args) {
        try {


            // it was modified -   // Locate the RMI registry
            Registry registry = LocateRegistry.getRegistry("localhost", 1099);

            // Look up the remote object
            NodeCLI.NodeInterface stub = (NodeCLI.NodeInterface) registry.lookup("Node");

            // Creează și exportă obiectul callback pentru client
            ClientApp clientApp = new ClientApp(); // Already exported in constructor
            ClientCallbackInterface callbackStub = (ClientCallbackInterface) clientApp;

            // Înregistrare callback la server
            // stub.registerCallback(callbackStub);

            Scanner scanner = new Scanner(System.in);

            while (true) {
                System.out.print("Enter command: ");
                String command = scanner.nextLine().trim();
                System.out.println("Command entered: " + command);  // Debug output

                if (command.equalsIgnoreCase("exit")) {
                    break;
                } else if (command.startsWith("add -agent")) {
                    // Verifică și parsează comanda
                    String[] parts = command.split(" ", 5); // split to maximum 5 parts
                    for (int i = 0; i < parts.length; i++) {
                        System.out.println("Part " + i + ": '" + parts[i] + "'");  // Debug output
                    }

                    if (parts.length == 5 && parts[3].equalsIgnoreCase("-shard")) {
                        String agentName = parts[2];
                        String shardName = parts[4];

                        // Folosește apeluri RMI pentru a adăuga un agent
                        stub.addAgent(agentName, shardName);
                        System.out.println("Agent added successfully: " + agentName + " to shard: " + shardName);
                    } else {
                        System.out.println("Invalid command format. Usage: add -agent AgentName -shard ShardName");
                    }
                }
                else if (command.equalsIgnoreCase("list agents")) {
                    // Command to list added agents
                    Map<String, String> agents = stub.listEntities();
                    if (agents.isEmpty()) {
                        System.out.println("No agents added.");
                    } else {
                        agents.forEach((agent, shard) ->
                                System.out.println("Agent: " + agent + ", Shard: " + shard));
                    }
                } else {
                    System.out.println("Unknown command. Valid commands: add -agent AgentName -shard ShardName, exit");
                }
                // need to add more commands as needed
            }

            scanner.close();
        } catch (Exception e) {
            System.err.println("Client exception: " + e.toString());
            e.printStackTrace();
        }
    }
}

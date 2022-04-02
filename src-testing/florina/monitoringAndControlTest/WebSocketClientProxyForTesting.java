package florina.monitoringAndControlTest;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;

import florina.monitoringAndControlTest.shards.PingTestComponent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.support.MessageReceiver;

/**
 * The {@link WebSocketClientProxyForTesting} manages communication with the server.
 *
 *  @author Florina Nastasoiu
 */
public class WebSocketClientProxyForTesting extends Unit {
    {
        setUnitName("websocket-client").setLoggerType(PlatformUtils.platformLogType());
    }

    protected WebSocketClient client;

    protected HashMap<String, MessageReceiver> messageReceivers	= new HashMap<>();

    void addReceiverAgent(String name, MessageReceiver receiver) {
        messageReceivers.put(name, receiver);
    }

    public WebSocketClientProxyForTesting(URI serverURI) {
        client = new WebSocketClient(serverURI) {

            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                li("new connection to server.");
            }

            /**
             * Receives a message from the server. The message was previously routed to this websocket client address
             * and it is further routed to a specific entity using the {@link MessageReceiver} instance. The entity is
             * searched within the context of this support.
             *
             * @param s
             *          - the JSON string containing a message and routing information
             */
            @Override
            public void onMessage(String s) {
                Object obj = JSONValue.parse(s);
                if(obj == null) {
                    le("null message received");
                    return;
                }
                JSONObject jsonObject = (JSONObject) obj;

                if(jsonObject.get("destination") == null) {
                    le("No destination entity received.");
                    return;
                }
                String destination = (String) jsonObject.get("destination");
                String localAddr = destination.split(
                        AgentWave.ADDRESS_SEPARATOR)[0];
                if(!messageReceivers.containsKey(localAddr) || messageReceivers.get(localAddr) == null)
                    le("Entity [] does not exist.", localAddr);
                else {
                    String source  = (String) jsonObject.get("source");
                    String content = (String) jsonObject.get("content");
                    messageReceivers.get(localAddr).receive(source, destination, content);
                    if (localAddr.equals("AgentA") && content != null && content.contains("reply") && source.contains("pong")) {
                        int ieo = source.indexOf("/");
                        if (ieo != -1) {
                            int index = Integer.parseInt(source.substring(0 , ieo));
                            PingTestComponent.lock_stopAgent.lock();
                            PingTestComponent.stopAgentsTime[index] = System.currentTimeMillis();
                            PingTestComponent.lock_stopAgent.unlock();
                        }
                    }
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                lw("Closed with exit code " + i);
            }

            @Override
            public void onError(Exception e) {
                le(Arrays.toString(e.getStackTrace()));
            }
        };
    }

    public void send(String message)
    {
        client.send(message);
    }

    public boolean connectBlocking() throws InterruptedException {
        return client.connectBlocking();
    }

    public void close() throws InterruptedException {
        client.closeBlocking();
    }
}

package websocketTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import websockets.WebSocketMessaging;
import websockets.WebSocketPylon;

public class Main
{
    public static void main(String[] args)
    {
		WebSocketPylon pylon = new WebSocketPylon();
		pylon.configure(
				new MultiTreeMap().addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885")
						.addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_PORT_NAME, "8885"));
		pylon.start();
        AgentTest one = new AgentTest("One");
        one.addContext(pylon.asContext());
        one.addMessagingShard(new WebSocketMessaging());


		WebSocketPylon pylon2 = new WebSocketPylon();
		pylon2.configure(
				new MultiTreeMap().addSingleValue(WebSocketPylon.WEBSOCKET_SERVER_ADDRESS_NAME, "ws://localhost:8885"));
		pylon2.start();
		AgentTest two = new AgentTest("Two");
        two.addContext(pylon2.asContext());
        two.addMessagingShard(new WebSocketMessaging());

        one.start();
        two.start();
    }

}
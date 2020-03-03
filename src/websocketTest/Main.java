package websocketTest;
import websockets.AgentTest;
import websockets.WebSocketPylon;
import websockets.WebSocketPylon.WebSocketLocalMessaging;

public class Main
{
    public static void main(String[] args)
    {
        WebSocketPylon pylon = new WebSocketPylon("ws://localhost:8886");

        AgentTest one = new AgentTest("One");
        one.addContext(pylon.asContext());
        AgentTest two = new AgentTest("Two");
        two.addContext(pylon.asContext());

        one.addMessagingShard(new WebSocketLocalMessaging());
        two.addMessagingShard(new WebSocketLocalMessaging());

        one.start();
        two.start();
    }

}
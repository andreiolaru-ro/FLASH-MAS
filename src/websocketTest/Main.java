package websocketTest;
import websockets.WebSocketPylon;
import websockets.WebSocketPylon.WebSocketMessaging;

public class Main
{
    public static void main(String[] args)
    {
        WebSocketPylon pylon = new WebSocketPylon("ws://localhost:8886");

        AgentTest one = new AgentTest("One");
        one.addContext(pylon.asContext());
        AgentTest two = new AgentTest("Two");
        two.addContext(pylon.asContext());

        one.addMessagingShard(new WebSocketMessaging());
        two.addMessagingShard(new WebSocketMessaging());

        one.start();
        two.start();
    }

}
package websocketTest;
import websockets.WebSocketPylon;
import websockets.WebSocketPylon.WebSocketMessaging;

public class Main
{
    public static void main(String[] args)
    {
        WebSocketPylon pylon = new WebSocketPylon("ws://localhost:8885");
        AgentTest one = new AgentTest("One");
        one.addContext(pylon.asContext());
        one.addMessagingShard(new WebSocketMessaging());


        WebSocketPylon pylon2 = new WebSocketPylon("ws://localhost:8885");
        AgentTest two = new AgentTest("Two");
        two.addContext(pylon2.asContext());
        two.addMessagingShard(new WebSocketMessaging());

        one.start();
        two.start();
    }

}
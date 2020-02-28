package websocketsTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;

import java.net.URI;
import java.net.URISyntaxException;

public class Main {
    public static void main(String[] args) throws URISyntaxException, InterruptedException {

        LocalSupport pylon = new LocalSupport();
        pylon.configure(new MultiTreeMap().addSingleValue(pylon.SERVER_ADDRESS, "ws://localhost:8883"));

        AgentClient one = new AgentClient(new URI(pylon.getServerAddressName()));
        one.setName("One");
        one.addContext(pylon.asContext());
        one.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
        one.connect();
        Thread.sleep(1000);
        String name = "name=" + one.getName();
        one.send(name);

        LocalSupport pylon2 = new LocalSupport();
        pylon2.configure(new MultiTreeMap().addSingleValue(pylon2.SERVER_ADDRESS, "ws://localhost:8883"));

        AgentClient two = new AgentClient(new URI(pylon2.getServerAddressName()));
        two.setName("Two");
        two.addContext(pylon2.asContext());
        two.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
        two.connect();
        Thread.sleep(1000);
        String name2 = "name=" + two.getName();
        two.send(name2);

        boolean _one = one.start();
        if (!_one) {
            Thread.sleep(1000);
            //source/destination/message
            String in = one.getName() + "/" + "Two" + "/" + "HELLO FROM THE OTHER SIDE!!";
            one.send(in);
        }
    }
}


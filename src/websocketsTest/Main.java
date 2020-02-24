package websocketsTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.Thread.enumerate;
import static java.lang.Thread.sleep;

public class Main {
    public static void main(String[] args) throws URISyntaxException, IOException, InterruptedException {

        LocalSupport pylon = new LocalSupport();
        pylon.configure(new MultiTreeMap().addSingleValue(pylon.SERVER_ADDRESS, "ws://localhost:8882"));

        AgentClient one = new AgentClient(new URI(pylon.getServerAddressName()));
        one.setName("One");
        one.addContext(pylon.asContext());
        one.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
        one.connect();
        sleep(10000);
        String name = "name=" + one.getName();
        one.send(name);

        LocalSupport pylon2 = new LocalSupport();
        pylon2.configure(new MultiTreeMap().addSingleValue(pylon2.SERVER_ADDRESS, "ws://localhost:8882"));

        AgentClient two = new AgentClient(new URI(pylon2.getServerAddressName()));
        two.setName("Two");
        two.addContext(pylon2.asContext());
        two.addMessagingShard(new LocalSupport.SimpleLocalMessaging());
        two.connect();
        sleep(10000);
        String name2 = "name=" + two.getName();
        two.send(name2);

        boolean _one = one.start();
        if (!_one) {
            sleep(10000);
            //source/destination/message
            String in = one.getName() + "/" + "Two" + "/" + "HELLO FROM THE OTHER SIDE!!";
            one.send(in);
        }
    }
}


package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.FileWriter;
import java.io.IOException;


public class MonitoringEntity extends Unit implements Entity {

    public MessageReceiver inbox;
    private FileWriter myWriter;

    public MonitoringEntity(String name) {
        {
            setUnitName(name);
            setLoggerType(PlatformUtils.platformLogType());
        }
        inbox = new MessageReceiver() {
            @Override
            public void receive(String source, String destination, String content) {
                Object obj = JSONValue.parse(content);
                if(obj == null) return;
                JSONObject mesg = (JSONObject) obj;
                String type = (String) mesg.get("action");
                String log = null;
                switch (MessageFactory.ActionType.valueOf(type)) {
                    case RECEIVE_MESSAGE:
                        log = "Agent " + destination + " received message " + mesg.get("content") + " from agent " + source;
                        break;
                    case MOVE_TO_ANOTHER_NODE:
                        log = "Agent " + source +" is leaving";
                        break;
                    case SEND_MESSAGE:
                        log = "Agent " + source + " sends message " + mesg.get("content") + " to agent " + destination;
                        break;
                    case ARRIVED_ON_NODE:
                        log = "Agent " + source + " arrived on " + destination;
                        break;
                    default:
                        break;
                }
                try {
                    myWriter.write(log + "\n");
                } catch (IOException e) {
                    le("An error occurred.");
                    e.printStackTrace();
                }
            }
        };

    }

    @Override
    public boolean start() {
        try {
            String filename = "log-" + getName() + ".txt";
            myWriter = new FileWriter(filename);
        } catch (IOException e) {
            le("An error occurred.");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean stop() {
        try {
            myWriter.close();
        } catch (IOException e) {
            le("An error occurred.");
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getName() {
        return getUnitName();
    }

    @Override
    public boolean addContext(EntityProxy context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy context) {
        return false;
    }

    @Override
    public EntityProxy asContext() {
        return null;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy context) {
        if(!(context instanceof MessagingPylonProxy))
            return false;
        MessagingPylonProxy pylon = (MessagingPylonProxy) context;
        return true;
    }
}

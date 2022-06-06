package net.xqhs.flash.shadowProtocol;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;


public class MonitoringEntity extends Unit implements Entity {

    public MessageReceiver inbox;
    private FileWriter myWriter;
    private List<Map<String, String>> logList;
    private Yaml yaml;

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

                Map<String, String> newLog = new HashMap<>();
                newLog.put("time", String.valueOf(mesg.get("time")));
                newLog.put("source", source);
                newLog.put("action", type);
                newLog.put("destination", destination);
                newLog.put("content", String.valueOf(mesg.get("content")));
                logList.add(newLog);
                System.out.println(logList);
                yaml.dump(newLog, myWriter);
//                if (logList.size() > 1) {
//                    yaml.dump(logList, myWriter);
//                    logList.clear();
//                }
            }
        };

    }

    @Override
    public boolean start() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        yaml = new Yaml();

        try {
            String filename = "log-" + getName() + ".yaml";
            myWriter = new FileWriter(filename, false);
        } catch (IOException e) {
            le("An error occurred.");
            e.printStackTrace();
        }
        logList = Collections.synchronizedList(new ArrayList<>());
        return false;
    }

    @Override
    public boolean stop() {
        yaml.dump(logList, myWriter);
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

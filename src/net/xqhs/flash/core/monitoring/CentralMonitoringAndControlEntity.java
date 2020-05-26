package net.xqhs.flash.core.monitoring;

import net.xqhs.flash.core.monitoring.gui.GUIBoard;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class CentralMonitoringAndControlEntity extends Unit implements  Entity<Pylon> {

    {
        setUnitName("monitoring-and-control-entity").setLoggerType(PlatformUtils.platformLogType());
    }
    /**
     * Endpoint element for this shard.
     */
    protected static final String	SHARD_ENDPOINT				        = "control";

    /**
     * Endpoint element for shards of control.
     */
    protected static final String   OTHER_CONTROL_SHARD_ENDPOINT        = "control";

    private MessagingShard          centralMessagingShard;

    private String                  name;

    private GUIBoard                gui;

    private static boolean          isRunning;

    /**
     * Keeps track of all agents deployed in the system and operations.
     */
    private HashMap<String, List<String>> allAgents = new LinkedHashMap<>();

    /*
    * Keeps track of all entities deployed in the system and supported operations along with their inner details.
    * */
    private HashMap<String, JSONArray> entitiesToOp = new LinkedHashMap<>();

    /**
     * Keeps track of entities state.
     * */
    private HashMap<String, String> entitiesState = new LinkedHashMap<>();

    /**
     * Keeps track of all nodes deployed in the system, along with their entities,
     * indexed by their names.
     */
    private HashMap<String, HashMap<String, List<String>>> allNodeEntities = new LinkedHashMap<>();

    public CentralMonitoringAndControlEntity(String name) {
        this.name = name;
    }

    public ShardContainer          proxy = new ShardContainer() {

        @Override
        public void postAgentEvent(AgentEvent event) {
            switch (event.getType())
            {
                case AGENT_WAVE:
                    String source = ((AgentWave) event).getFirstSource();
                    String content = ((AgentWave) event).getContent();
                    Object obj = JSONValue.parse(content);
                    if(obj == null)
                        le("null message from [].", source);
                    if(parseJSON(obj))
                        li("Registered entities from [].", source);
                    break;
                default:
                    break;
            }
        }

        public boolean parseJSON(Object obj)
        {
            if(obj instanceof JSONObject) {
                JSONObject jsonObj = (JSONObject) obj;
                if(jsonObj.get("operation") != null)
                    return  manageOperation(jsonObj);
            }
            if(obj instanceof JSONArray) {
                JSONArray ja = (JSONArray)obj;
                return registerEntities(ja);
            }
            return false;
        }

        private boolean manageOperation(JSONObject jsonObj) {
            String op = (String) jsonObj.get("operation");
            if(op.equals("state-update")) {
                String params = (String) jsonObj.get("params");
                String value  = (String) jsonObj.get("value");
                System.out.println(params + " ### " + value);
                entitiesState.put(params, value);
                return true;
            }
            return false;
        }

        private boolean registerEntities(JSONArray ja) {
            for (Object o : ja) {
                JSONObject entity   = (JSONObject) o;
                String node         = (String)entity.get("node");
                String category     = (String)entity.get("category");
                String name         = (String)entity.get("name");

                JSONArray operationDetails = (JSONArray) entity.get("operations");
                if(category.equals("agent"))
                {
                    if(!allAgents.containsKey(name))
                        allAgents.put(name, new LinkedList<>());
                    for(Object oo : operationDetails) {
                        JSONObject op   = (JSONObject) oo;
                        String operation = (String) op.get("name");
                        allAgents.get(name).add(operation);
                    }
                }
                entitiesToOp.put(name, operationDetails);

                if(!allNodeEntities.containsKey(node))
                    allNodeEntities.put(node, new LinkedHashMap<>());
                if(!allNodeEntities.get(node).containsKey(category))
                    allNodeEntities.get(node).put(category, new LinkedList<>());
                allNodeEntities.get(node).get(category).add(name);
            }
            return true;
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation) {
            return null;
        }

        @Override
        public String getEntityName() {
            return getName();
        }
    };


    /**
     * @param childEntity
     *                      - an ordinary entity.
     * @return
     *                      - the name of its parent in context hierarchy - as a node - .
     */
    public String getParentNode(String childEntity) {
        for (Map.Entry<String, HashMap<String, List<String>>> entry : allNodeEntities.entrySet()) {
            for (Map.Entry<String, List<String>> stringListEntry : entry.getValue().entrySet()) {
                if (stringListEntry.getValue().contains(childEntity))
                    return entry.getKey();
            }
        }
        return null;
    }

    /**
     * @return
     *          - a {@link List} of all nodes deployed in the system by their names
     */
    public List<String> getNodes() {
        List<String> nodes = new LinkedList<>();
        allNodeEntities.entrySet().forEach(entry-> {
            nodes.add(entry.getKey());
        });
        return nodes;
    }

    @Override
    public boolean start() {
        if(centralMessagingShard == null){
            le("[] unable to start. No messaging shard found.", getName());
            return false;
        }
        centralMessagingShard.registerCentralEntity(name);
        isRunning = true;
        li("[] started successfully.", getName());

        if(canStartGuiBoard())
            li("[] launched.", gui.getName());
        return true;
    }

    public boolean canStartGuiBoard() {
        gui = new GUIBoard(new CentralEntityProxy());
        SwingUtilities.invokeLater(() -> {
            try {
                gui.setVisible(true);
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        li("[] stopped successfully.", getName());
        isRunning = false;
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        PylonProxy pylonProxy = (PylonProxy) context;
        String recommendedShard = pylonProxy
                .getRecommendedShardImplementation(
                        AgentShardDesignation.standardShard(
                                AgentShardDesignation.StandardAgentShard.MESSAGING));
        try
        {
            centralMessagingShard = (MessagingShard) PlatformUtils
                    .getClassFactory()
                    .loadClassInstance(recommendedShard, null, true);
        } catch(ClassNotFoundException
                | InstantiationException | NoSuchMethodException
                | IllegalAccessException | InvocationTargetException e)
        {
            e.printStackTrace();
        }
        centralMessagingShard.addContext(proxy);
        return centralMessagingShard.addGeneralContext(context);
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return addContext((MessagingPylonProxy) context);
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return null;
    }

    public class CentralEntityProxy {
        /**
         * @param entityName
         *                  - the name of destination entity
         * @param command
         *                  - control command
         * @return
         *                  - an indication of success
         */
        public boolean isGuiCommandSent(String entityName, String command) {
            //TODO: Change this if necessary
            return centralMessagingShard
                    .sendMessage(
                            AgentWave.makePath(getName(), SHARD_ENDPOINT),
                            AgentWave.makePath(entityName, OTHER_CONTROL_SHARD_ENDPOINT),
                            command);
        }

        /**
         *
         * @param command
         *                  - command to be sent to all agents running in the system.
         */
        public void sendToAll(String command) {
            allAgents.entrySet().forEach(entry -> {
                if(!sendTo(entry.getKey(), command)) return;
            });
        }

        public boolean sendTo(String destination, String command) {
            JSONObject cmdJson = getCommandJson(destination, command);
            if(cmdJson == null) {
                le("Entity [] does not support [] command.", destination, command);
                return false;
            }
            String access = (String) cmdJson.get("access");
            if(access.equals("self")) {
                if(!sendControlCommand(destination, command)) {
                    le("Message from [] to [] failed.", getName(), destination);
                    return false;
                }
            } else {
                String proxy = (String) cmdJson.get("proxy");
                JSONObject routedCommand = new JSONObject();
                routedCommand.put("child", destination);
                routedCommand.put("operation", command);
                if(!sendControlCommand(proxy, routedCommand.toString())) {
                    le("Message from [] to proxy [] of [] failed.", getName(), proxy, destination);
                    return false;
                }
            }
            return true;
        }

        private JSONObject getCommandJson(String name, String command) {
            JSONArray ja = entitiesToOp.get(name);
            for(Object o : ja) {
                JSONObject op   = (JSONObject) o;
                String cmd = (String) op.get("name");
                if(cmd.equals(command))
                    return op;
            }
            return null;
        }

        private boolean sendControlCommand(String destination, String content) {
            return centralMessagingShard.sendMessage(
                    AgentWave.makePath(getName(), SHARD_ENDPOINT),
                    AgentWave.makePath(destination, OTHER_CONTROL_SHARD_ENDPOINT),
                    content);
        }
    }
}

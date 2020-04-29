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

    protected static final String	SHARD_ENDPOINT				        = "control";

    protected static final String	OTHER_SHARD_ENDPOINT				= "control";

    private MessagingShard          centralMessagingShard;

    private String                  name;

    private GUIBoard                gui;

    private static boolean          isRunning;

    /**
     * Keeps track of all agents deployed in the system and operations which can be done by agents themselves.
     */
    private HashMap<String, List<String>> allAgents = new LinkedHashMap<>();

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
                    String content = ((AgentWave) event).getContent();
                    String source = ((AgentWave) event).getFirstSource();
                    Object obj = JSONValue.parse(content);
                    if(obj == null)
                        le("null message from [].", source);
                    if(parseJSON(obj))
                        li("Entities from [] registered.", source);
                    break;
                default:
                    break;
            }
        }

        public boolean parseJSON(Object obj)
        {
            // Here comes the entities registered in the context of a node. Being a bunch of entities,
            // they always come in json array format.
            if(obj instanceof JSONArray)
            {
                JSONArray ja = (JSONArray)obj;
                for (Object o : ja) {
                    JSONObject entity   = (JSONObject) o;
                    String node         = (String)entity.get("node");
                    String category     = (String)entity.get("category");
                    String name         = (String)entity.get("name");
                    String[] operations = ((String)entity.get("operations")).split(" ");

                    if(category.equals("agent"))
                    {
                        if(!allAgents.containsKey(name))
                            allAgents.put(name, new LinkedList<>());
                        allAgents.get(name).addAll(Arrays.asList(operations));
                    }
                    if(!allNodeEntities.containsKey(node))
                        allNodeEntities.put(node, new LinkedHashMap<>());
                    if(!allNodeEntities.get(node).containsKey(category))
                        allNodeEntities.get(node).put(category, new LinkedList<>());
                    allNodeEntities.get(node).get(category).add(name);
                }
                return true;
            }
            return false;
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
        return true;
    }

    public void startGUIBoard() {
        SwingUtilities.invokeLater(() -> {
            try {
                gui = new GUIBoard(this);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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


    /**
     * @param entityName
     *                  - the name of destination entity
     * @param command
     *                  - control command
     * @return
     *                  - an indication of success
     */
    public boolean sendGUICommand(String entityName, String command) {
        //TODO: Change this if necessary
        return centralMessagingShard
                .sendMessage(
                        AgentWave.makePath(getName(), SHARD_ENDPOINT),
                        AgentWave.makePath(entityName, OTHER_SHARD_ENDPOINT),
                        command);
    }

    public boolean sendToAllAgents(String command) {
        //TODO:
        return true;
    }
}

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
import net.xqhs.flash.core.util.OperationUtils;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Unit;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import web.WebEntity;

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
     * Keeps track of all agents deployed in the system and their {@link List} of operations.
     */
    private HashMap<String, List<String>> allAgents = new LinkedHashMap<>();

    /**
     * Keeps track of all entities deployed in the system and their {@link JSONArray} of operations.
     */
    private HashMap<String, JSONArray> entitiesToOp = new LinkedHashMap<>();

    /**
     * Keeps track of entities state.
     * */
    private HashMap<String, String> entitiesState = new LinkedHashMap<>();

    /**
     * Keeps track of all nodes deployed in the system, along with their {@link List} of entities,
     * indexed by their categories and names.
     */
    private HashMap<String, HashMap<String, List<String>>> allNodeEntities = new LinkedHashMap<>();

    public CentralMonitoringAndControlEntity(String name) {
        this.name = name;
        if(canStartGuiBoard())
            li("[] launched.", gui.getName());
    }

    public ShardContainer          proxy = new ShardContainer() {

        @Override
        public void postAgentEvent(AgentEvent event) {
            switch (event.getType()) {
                case AGENT_WAVE:
                    String source = ((AgentWave) event).getFirstSource();
                    String content = ((AgentWave) event).getContent();
                    Object obj = JSONValue.parse(content);
                    if(obj == null) {
                        le("null message from [].", source);
                        break;
                    }
                    if(parseReceivedMsg(obj, source))
                        li("Parsed message from [].", source);
                    break;
                default:
                    break;
            }
        }

        /**
         * @param obj
         *              - the object received as content through the {@link ShardContainer}
         * @param source
         *              - the source of the message
         * @return
         *              - an indication of success
         */
        public boolean parseReceivedMsg(Object obj, String source)
        {
            if(obj instanceof JSONObject) {
                JSONObject jo = (JSONObject) obj;
                if(manageOperation(jo)) {
                    li("Parsed operation from [].", source);
                    return true;
                }
            }
            if(obj instanceof JSONArray) {
                JSONArray ja = (JSONArray)obj;
                if(registerEntities(ja)) {
                    li("Registered entities from [].", source);
                    return true;
                }
            }
            return false;
        }

        /**
         * This analysis the operation received and performs it.
         * @param jsonObj
         *                  - the object received as content through the {@link ShardContainer}
         * @return
         *                  - an indication of success
         */
        private boolean manageOperation(JSONObject jsonObj) {
            String op = (String) jsonObj.get(OperationUtils.NAME);
            if(op.equals("state-update")) {
                String params = (String) jsonObj.get("params");
                String value  = (String) jsonObj.get("value");
                entitiesState.put(params, value);
                SwingUtilities.invokeLater(() -> {
                    try {
                        gui.updateStateOfEntity(params, value);
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                });
                return true;
            }
            return false;
        }

        private boolean registerEntities(JSONArray ja) {
            for (Object o : ja) {
                JSONObject entity   = (JSONObject) o;
                String node         = (String)entity.get(OperationUtils.NODE);
                String category     = (String)entity.get(OperationUtils.CATEGORY);
                String name         = (String)entity.get(OperationUtils.NAME);

                JSONArray operationDetails = (JSONArray) entity.get(OperationUtils.OPERATIONS);
                if(category.equals("agent"))
                {
                    if(!allAgents.containsKey(name))
                        allAgents.put(name, new LinkedList<>());
                    for(Object oo : operationDetails) {
                        JSONObject op   = (JSONObject) oo;
                        String operation = (String) op.get(OperationUtils.NAME);
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
        centralMessagingShard.register(name);
        isRunning = true;
        li("[] started successfully.", getName());
        return true;
    }

    public boolean canStartGuiBoard() {
        gui = new GUIBoard(new CentralEntityProxy());
        WebEntity.cep = new CentralEntityProxy();
        new WebEntity().start();
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
         *
         * @param operation
         *                  - command to be sent to all agents registered
         */
        public void sendToAllAgents(String operation) {
            allAgents.entrySet().forEach(entry -> {
                if(!sendToEntity(entry.getKey(), operation)) return;
            });
        }

        /**
         * Sends a control message to a specific entity which will further perform the operation.
         * @param destination
         *                      - the name of destination entity
         * @param operation
         *                      - operation to be performed on this entity
         * @return
         *                      - an indication of success
         */
        public boolean sendToEntity(String destination, String operation) {
            JSONObject jsonOperation = getOperationFromEntity(destination, operation);
            if(jsonOperation == null) {
                le("Entity [] does not exist or does not support [] command.", destination, operation);
                return false;
            }
            String access = (String) jsonOperation.get("access");
            if(access.equals("self")) {
                JSONObject msg = OperationUtils.operationToJSON(operation, destination, "", destination);
                if(!sendMessage(destination, msg.toString())) {
                    le("Message from [] to [] failed.", getName(), destination);
                    return false;
                }
            } else {
                String proxy = (String) jsonOperation.get("proxy");
                JSONObject msg = OperationUtils.operationToJSON(operation, proxy, "", destination);
                if(!sendMessage(proxy, msg.toString())) {
                    le("Message from [] to proxy [] of [] failed.", getName(), proxy, destination);
                    return false;
                }
            }
            return true;
        }

        private JSONObject getOperationFromEntity(String entity, String command) {
            JSONArray ja = entitiesToOp.get(entity);
            if(ja == null) return null;
            for(Object o : ja) {
                JSONObject op   = (JSONObject) o;
                String cmd = (String) op.get("name");
                if(cmd.equals(command))
                    return op;
            }
            return null;
        }

        /**
         * @param destination
         *                      - the name of the destination entity
         * @param content
         *                      - the content to be sent
         * @return
         *                      - an indication of success
         */
        private boolean sendMessage(String destination, String content) {
            return centralMessagingShard.sendMessage(
                    AgentWave.makePath(getName(), SHARD_ENDPOINT),
                    AgentWave.makePath(destination, OTHER_CONTROL_SHARD_ENDPOINT),
                    content);
        }

        public String getEntities() {
            JSONObject entities = new JSONObject();
            allNodeEntities.entrySet().forEach(consumer -> {
                String node = consumer.getKey();
                entities.put(node, "node " + entitiesState.get(node));
                consumer.getValue().entrySet().forEach(entry -> {
                    String category = entry.getKey();
                    entry.getValue().forEach(entity -> {
                        entities.put(entity, category + " " + entitiesState.get(entity) + " " + entitiesToOp.get(entity).toString());
                    });
                });
            });
            return entities.toString();
        }
    }
}

package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.implem.DefaultFMasImplementation;
import net.xqhs.flash.ent_op.implem.EntityToolsImplementation;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static net.xqhs.flash.ent_op.entities.PingPongOperation.PING_PONG_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class PingPongAgent extends Unit implements EntityAPI {

    /**
     * The name of the component parameter that contains the id of the other agent.
     */
    public static final String DEST_AGENT_PARAMETER_NAME = "sendTo";

    /**
     * Initial delay before the first ping message.
     */
    private static final long PING_INITIAL_DELAY = 2000;

    /**
     * Time between ping messages.
     */
    private static final long PING_PERIOD = 2000;

    /**
     * Indicates whether the implementation is currently running.
     */
    private boolean isRunning;

    /**
     * The name of the agent.
     */
    private String agentName;

    /**
     * Cache for the name of the other agent.
     */
    private List<String> otherAgents;

    /**
     * Timer for pinging.
     */
    private Timer pingTimer;

    /**
     * The id of this instance.
     */
    private EntityID entityID;

    /**
     * The corresponding entity tools for this instance.
     */
    private EntityToolsImplementation entityTools;

    @Override
    public boolean setup(MultiTreeMap configuration) {
        agentName = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
        entityID = new EntityID(configuration.getAValue(ENTITY_ID_ATTRIBUTE_NAME));
        entityTools = new EntityToolsImplementation();
        entityTools.initialize(this);
        DefaultFMasImplementation.getInstance().registerEntity(entityID.ID, entityTools);
        setUnitName(agentName);
        if (configuration.isSet(DEST_AGENT_PARAMETER_NAME))
            otherAgents = configuration.getValues(DEST_AGENT_PARAMETER_NAME);
        return true;
    }

    @Override
    public boolean start() {
        isRunning = true;
        li("Agent [] started", agentName);
        if (otherAgents != null) {
            pingTimer = new Timer();
            pingTimer.schedule(new TimerTask() {
                /**
                 * The index of the message sent.
                 */
                int tick = 0;

                @Override
                public void run() {
                    tick++;
                    for (String otherAgent : otherAgents) {
                        lf("Sending the message to ", otherAgent);
                        EntityID otherAgentID = new EntityID(otherAgent);
                        ArrayList<Object> argumentValues = new ArrayList<>();
                        argumentValues.add("ping-no " + tick);
                        OperationCall pingPongOpCall = new OperationCall(entityID, otherAgentID,
                                PING_PONG_OPERATION_NAME, false, argumentValues);
                        callOperation(pingPongOpCall);
                    }
                }
            }, PING_INITIAL_DELAY, PING_PERIOD);
        }
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public boolean stop() {
        pingTimer.cancel();
        li("Agent [] stopped", agentName);
        return true;
    }

    @Override
    public Object handleOperationCall(OperationCall operationCall) {
        if (!isRunning) {
            le("[] is not running", agentName);
            return null;
        }
        if (operationCall.getOperationName().equals(PING_PONG_OPERATION_NAME)) {
            String message = operationCall.getArgumentValues().get(0).toString();
            String sender = operationCall.getSourceEntity().ID;
            li("received message: [] from []", message, sender);

            if (otherAgents == null) {
                ArrayList<Object> argumentValues = new ArrayList<>();
                argumentValues.add(message + " reply");
                OperationCall replyOpCall = new OperationCall(entityID, operationCall.getSourceEntity(),
                        PING_PONG_OPERATION_NAME, false, argumentValues);
                callOperation(replyOpCall);
            }
        }
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return agentName;
    }

    public void callOperation(OperationCall operationCall) {
        entityTools.handleOutgoingOperationCall(operationCall);
    }

    public EntityTools getEntityTools() {
        return entityTools;
    }
}

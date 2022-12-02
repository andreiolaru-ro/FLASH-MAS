package net.xqhs.flash.ent_op.entities.agent;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.operations.PingPongOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.PingPongOperation.PING_PONG_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class PingPongAgent extends Unit implements EntityAPI {

    /**
     * The name of the component parameter that contains the id of the other agent.
     */
    public static final String DEST_AGENT_PARAMETER_NAME = "sendTo";

    /**
     * Initial delay before the first ping message.
     */
    protected static final long PING_INITIAL_DELAY = 2000;

    /**
     * Time between ping messages.
     */
    protected static final long PING_PERIOD = 2000;

    /**
     * Indicates whether the implementation is currently running.
     */
    protected boolean isRunning;

    /**
     * The name of the agent.
     */
    protected String agentName;

    /**
     * Cache for the name of the other agent.
     */
    protected List<String> otherAgents;

    /**
     * Timer for pinging.
     */
    protected Timer pingTimer;

    /**
     * The id of this instance.
     */
    protected EntityID entityID;

    /**
     * The corresponding entity tools for this instance.
     */
    protected EntityTools entityTools;

    /**
     * The ping pong operation.
     */
    protected Operation pingPong;

    /**
     * The framework instance.
     */
    protected FMas fMas;

    public PingPongAgent(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean setup(MultiTreeMap configuration) {
        agentName = configuration.getAValue(NAME_ATTRIBUTE_NAME);
        entityID = new EntityID(configuration.getAValue(ENTITY_ID_ATTRIBUTE_NAME));
        pingPong = new PingPongOperation();
        entityTools = fMas.registerEntity(this);
        entityTools.createOperation(pingPong);
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
                        OperationCallWave pingPongOpCall = new OperationCallWave(entityID, otherAgentID,
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
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        if (!isRunning) {
            le("[] is not running", agentName);
            return null;
        }
        if (operationCall.getTargetOperation().equals(PING_PONG_OPERATION_NAME)) {
            String message = operationCall.getArgumentValues().get(0).toString();
            String sender = operationCall.getSourceEntity().ID;
            li("received message: [] from []", message, sender);

            if (otherAgents == null) {
                ArrayList<Object> argumentValues = new ArrayList<>();
                argumentValues.add(message + " reply");
                OperationCallWave replyOpCall = new OperationCallWave(entityID, operationCall.getSourceEntity(),
                        PING_PONG_OPERATION_NAME, false, argumentValues);
                callOperation(replyOpCall);
            }
        }
        return null;
    }

    @Override
    public boolean changeRelation(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return agentName;
    }

    @Override
    public List<Operation> getOperations() {
        return null;
    }

    @Override
    public boolean canRoute(EntityID entityID) {
        return false;
    }

    @Override
    public EntityID getEntityID() {
        return entityID;
    }

    public void callOperation(OperationCallWave operationCall) {
        entityTools.handleOutgoingWave(operationCall);
    }

    public EntityTools getEntityTools() {
        return entityTools;
    }
}

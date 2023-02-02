package gabi.entityOperationTest;

import static net.xqhs.flash.ent_op.impl.operations.PingPongOperation.PING_PONG_OPERATION_NAME;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.operations.PingPongOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;

public class PingPongAgent extends Agent {

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
     * Cache for the name of the other agent.
     */
    protected List<String> otherAgents;

    /**
     * Timer for pinging.
     */
    protected Timer pingTimer;


    /**
     * The ping pong operation.
     */
    protected Operation pingPong;



    @Override
    public boolean setup(MultiTreeMap configuration) {
		super.setup(configuration);
		if(configuration.isSet(DEST_AGENT_PARAMETER_NAME))
			otherAgents = configuration.getValues(DEST_AGENT_PARAMETER_NAME);
		return true;
	}
	
	@Override
	public boolean connectTools(OutboundEntityTools entityTools) {
		super.connectTools(entityTools);
        pingPong = new PingPongOperation();
        entityTools.createOperation(pingPong);
        return true;
    }

    @Override
    public boolean start() {
        isRunning = true;
		li("Agent started");
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
		li("Agent stopped");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        if (!isRunning) {
			le("entity is not running");
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
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public boolean canRoute(EntityID entityID) {
        return false;
    }

    @Override
    public EntityID getID() {
        return entityID;
    }

    public void callOperation(OperationCallWave operationCall) {
		framework.handleOutgoingWave(operationCall);
    }
}

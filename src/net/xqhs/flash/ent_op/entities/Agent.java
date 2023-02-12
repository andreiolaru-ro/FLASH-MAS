package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.ent_op.impl.operations.ReceiveOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;

import static net.xqhs.flash.ent_op.impl.operations.ReceiveOperation.RECEIVE_OPERATION;

public class Agent extends EntityCore {
	@Override
	public boolean connectTools(OutboundEntityTools entityTools) {
		super.connectTools(entityTools);
		var receiveOp = new ReceiveOperation();
		entityTools.createOperation(receiveOp);
		return true;
	}

    @Override
    public boolean start() {
		super.start();
        li("Agent started");
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    public boolean stop() {
        isRunning = false;
        li("Agent stopped");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        if (operationCall.getTargetOperation().equals(RECEIVE_OPERATION)) {
            var message = operationCall.getArgumentValues().get(0).toString();
            var sender = operationCall.getSourceEntity().ID;
            li("############ received message: [] from []", message, sender);
        }
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return true;
    }

    @Override
    public boolean canRoute(EntityID entityID) {
        return false;
    }

}
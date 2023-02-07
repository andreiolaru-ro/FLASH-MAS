package gabi.entityOperationTest;

import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.ResultReceiver;

public class SimpleAgent extends Agent {

    public void callOperation(OperationCallWave operationCallWave) {
        framework.handleOutgoingWave(operationCallWave);
    }

    public void callOperationWithResult(OperationCallWave operationCallWave, ResultReceiver callBack) {
        framework.handleOutgoingWave(operationCallWave, callBack);
    }

    public void callRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        framework.changeRelation(changeType, relation);
    }
}

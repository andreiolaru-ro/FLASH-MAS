package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.SimpleAgent;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.ResultReceiver;

public class PhoneAgent extends SimpleAgent {
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

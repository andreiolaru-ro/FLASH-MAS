package gabi.entityOperationTest;

import net.xqhs.flash.ent_op.entities.EntityCore;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.Relation;

public class TestEntity extends EntityCore implements EntityAPI {

    /**
     * The default name for instances of this implementation.
     */
    protected static final String DEFAULT_NAME = "Default Test Entity";

    @Override
    public boolean start() {
        // does nothing, only changes the entity's state
        isRunning = true;
		li("started");
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCallWave) {
        if (!isRunning) {
			le("is not running");
            return null;
        }
        if ("PRINT".equals(operationCallWave.getTargetOperation())) {
            printMessageOperation(operationCallWave.getArgumentValues().get(0).toString());
        }
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    private void printMessageOperation(String message) {
        li("received message: []", message);
    }
}

package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.SimpleAgent;
import gabi.entityOperationTest.scenario.operations.AuthOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;

import static gabi.entityOperationTest.scenario.operations.AuthOperation.AUTH_OPERATION;
import static gabi.entityOperationTest.scenario.relations.ActivityRelation.getAllActivities;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.INSIDE_CLASSROOM;

public class ManagementAgent extends SimpleAgent {

    @Override
    public boolean connectTools(OutboundEntityTools entityTools) {
        super.connectTools(entityTools);
        entityTools.createOperation(new AuthOperation());
        return true;
    }

    @Override
    public boolean start() {
        super.start();
        li("Management Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        var operation = operationCall.getTargetOperation();

        if (AUTH_OPERATION.equals(operation)) {
            var operationCallToAuth = (OperationCallWave) operationCall.getArgumentValues().get(0);
            return handleAuthOperation(operationCallToAuth);
        }

        return operation + " operation is not supported by the " + getUnitName() + "entity.";
    }

    private boolean handleAuthOperation(OperationCallWave operationCall) {
        var sourceEntityId = operationCall.getSourceEntity();
        var hasActivity = framework.getOutgoingRelations().stream()
                .filter(relation -> relation.getTo().equals(sourceEntityId))
                .anyMatch(relation -> getAllActivities().contains(relation.getRelationName()));
        var isInsideClassroom = framework.getOutgoingRelations().stream()
                .filter(relation -> relation.getTo().equals(sourceEntityId))
                .anyMatch(relation -> relation.getRelationName().equals(INSIDE_CLASSROOM.toString()));
        return hasActivity && isInsideClassroom;
    }
}

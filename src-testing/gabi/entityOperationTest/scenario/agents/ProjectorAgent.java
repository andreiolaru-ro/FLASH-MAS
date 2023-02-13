package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.scenario.operations.ChangeSlideOperation;
import gabi.entityOperationTest.scenario.operations.EndPresentationOperation;
import gabi.entityOperationTest.scenario.operations.StartPresentationOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.waves.ResultWave;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;

import java.io.File;
import java.util.List;

import static gabi.entityOperationTest.scenario.agents.SmartHomeAgent.SystemState.ON;
import static gabi.entityOperationTest.scenario.operations.AuthOperation.AUTH_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;

public class ProjectorAgent extends SmartHomeAgent {

    private SystemState state;
    private File presentation;

    @Override
    public boolean connectTools(OutboundEntityTools entityTools) {
        super.connectTools(entityTools);
        entityTools.createOperation(new StartPresentationOperation());
        entityTools.createOperation(new EndPresentationOperation());
        entityTools.createOperation(new ChangeSlideOperation());
        return true;
    }

    @Override
    public boolean start() {
        super.start();
        state = SystemState.OFF;
        li("Projector Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        var operation = operationCall.getTargetOperation();
        var sourceEntityId = operationCall.getSourceEntity();
        var managerId = framework.getIncomingRelations().stream()
                .filter(relation -> relation.getRelationName().equalsIgnoreCase("MANAGER"))
                .map(Relation::getFrom)
                .findFirst()
                .get();
        var authWave = new OperationCallWave(getID(), managerId, AUTH_OPERATION, true, List.of(operationCall));

        framework.handleOutgoingWave(authWave, (authorized) -> {
            if (Boolean.parseBoolean(authorized.toString())) {
                switch (operationCall.getTargetOperation()) {
                    case TURN_ON_OPERATION:
                        state = ON;
                        var responseWave = new ResultWave(getID(), sourceEntityId, operationCall.getId(), "Projector was turned on.");
                        framework.handleOutgoingWave(responseWave);
                }
                li("authorized");
            } else {
               li("The operation is not authorized. Only teachers can control the heating.");
            }
        });

        return null;
    }

}

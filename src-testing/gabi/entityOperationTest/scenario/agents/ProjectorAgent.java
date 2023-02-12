package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.scenario.operations.ChangeSlideOperation;
import gabi.entityOperationTest.scenario.operations.EndPresentationOperation;
import gabi.entityOperationTest.scenario.operations.StartPresentationOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;

import java.io.File;

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
        var authWave = new OperationCallWave();

        framework.handleOutgoingWave(authWave, (authorized) -> {
            if (Boolean.parseBoolean(authorized.toString())) {
                li("authorized");
            } else {
               li("The operation is not authorized. Only teachers can control the heating.");
            }
        });

        return null;
    }

}

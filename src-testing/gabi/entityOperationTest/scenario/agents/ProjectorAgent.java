package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.scenario.operations.ChangeSlideOperation;
import gabi.entityOperationTest.scenario.operations.EndPresentationOperation;
import gabi.entityOperationTest.scenario.operations.ExportOperation;
import gabi.entityOperationTest.scenario.operations.StartPresentationOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.waves.ResultWave;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;

import java.io.File;
import java.util.List;

import static gabi.entityOperationTest.scenario.operations.AuthOperation.AUTH_OPERATION;
import static gabi.entityOperationTest.scenario.operations.ChangeSlideOperation.CHANGE_SLIDE_OPERATION;
import static gabi.entityOperationTest.scenario.operations.EndPresentationOperation.END_PRESENTATION_OPERATION;
import static gabi.entityOperationTest.scenario.operations.ExportOperation.EXPORT_OPERATION;
import static gabi.entityOperationTest.scenario.operations.StartPresentationOperation.START_PRESENTATION_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOffOperation.TURN_OFF_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.MANAGER;

public class ProjectorAgent extends SmartHomeAgent {

    private SystemState state;
    private SystemState presentationState;
    private File presentation;

    @Override
    public boolean connectTools(OutboundEntityTools entityTools) {
        super.connectTools(entityTools);
        entityTools.createOperation(new StartPresentationOperation());
        entityTools.createOperation(new EndPresentationOperation());
        entityTools.createOperation(new ExportOperation());
        entityTools.createOperation(new ChangeSlideOperation());
        return true;
    }

    @Override
    public boolean start() {
        super.start();
        state = SystemState.OFF;
        presentationState = SystemState.CLOSED;
        li("Projector Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        var sourceEntityId = operationCall.getSourceEntity();
        var managerId = framework.getIncomingRelations().stream()
                .filter(relation -> relation.getRelationName().equalsIgnoreCase(MANAGER.toString()))
                .map(Relation::getFrom)
                .findFirst()
                .get();
        var authWave = new OperationCallWave(getID(), managerId, AUTH_OPERATION, true, List.of(operationCall));

        framework.handleOutgoingWave(authWave, (authorized) -> {
            var response = "";
            if (Boolean.parseBoolean(authorized.toString())) {
                switch (operationCall.getTargetOperation()) {
                    case TURN_ON_OPERATION:
                        response = handleTurnOnOperation();
                        break;
                    case TURN_OFF_OPERATION:
                        response = handleTurnOffOperation();
                        break;
                    case START_PRESENTATION_OPERATION:
                        var filePath = operationCall.getArgumentValues().get(0).toString();
                        response = handleStartPresentationOperation(filePath);
                        break;
                    case CHANGE_SLIDE_OPERATION:
                        var slideNumber = Integer.parseInt(operationCall.getArgumentValues().get(0).toString());
                        response = handleChangeSlideOperation(slideNumber);
                        break;
                    case EXPORT_OPERATION:
                        var fileFormat = operationCall.getArgumentValues().get(0).toString();
                        response = handleExportOperation(fileFormat);
                        break;
                    case END_PRESENTATION_OPERATION:
                        response = handleEndPresentationOperation();
                        break;
                }
            } else {
                response = "The operation is not authorized. You should have an activity and be present in the classroom to control the projector.";
            }
            var responseWave = new ResultWave(getID(), sourceEntityId, operationCall.getId(), response);
            framework.handleOutgoingWave(responseWave);
        });

        return null;
    }

    private String handleTurnOnOperation() {
        if (state == SystemState.OFF) {
            state = SystemState.ON;
            return getID() + " The projector was turned on.";
        }
        return getID() + " The projector is already on.";
    }

    private String handleTurnOffOperation() {
        if (state == SystemState.ON) {
            state = SystemState.OFF;
            return getID() + " The projector was turned off.";
        }
        return getID() + " The projector is already off.";
    }

    private String handleStartPresentationOperation(String filePath) {
        if (state == SystemState.OFF) {
            return getID() + " The projector is turned off. Turn on the projector to start the presentation.";
        }
        presentationState = SystemState.OPEN;
        return getID() + " The " + filePath + " presentation has started.";
    }

    private String handleChangeSlideOperation(int slideNumber) {
        if (state == SystemState.OFF) {
            return getID() + " The projector is turned off. Turn on the projector to start a presentation.";
        }
        if (presentationState == SystemState.CLOSED) {
            return getID() + " Can't change the slide. There is currently no presentation in progress.";
        }
        return getID() + " The presentation was changed to slide " + slideNumber + ".";
    }

    private String handleExportOperation(String fileFormat) {
        if (presentationState == SystemState.CLOSED) {
            return getID() + " Can't export the presentation. There is currently no presentation in progress.";
        }
        return getID() + " The current presentation was exported as a " + fileFormat + " file.";
    }

    private String handleEndPresentationOperation() {
        if (state == SystemState.OFF) {
            return getID() + " The projector is turned off. Turn on the projector to start a presentation.";
        }
        if (presentationState == SystemState.CLOSED) {
            return getID() + " Can't end the presentation. There is currently no presentation in progress.";
        }
        presentationState = SystemState.CLOSED;
        return getID() + " The current presentation has ended.";
    }

}

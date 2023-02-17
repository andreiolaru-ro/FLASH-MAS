package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.scenario.operations.ExportOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.waves.ResultWave;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;

import java.util.List;

import static gabi.entityOperationTest.scenario.operations.AuthOperation.AUTH_OPERATION;
import static gabi.entityOperationTest.scenario.operations.ExportOperation.EXPORT_OPERATION;
import static gabi.entityOperationTest.scenario.operations.GetOperation.GET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.SetOperation.SET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOffOperation.TURN_OFF_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.MANAGER;

public class SmartBoardAgent extends SmartHomeAgent {
    private SystemState state;
    private double luminosity;

    @Override
    public boolean connectTools(OutboundEntityTools entityTools) {
        super.connectTools(entityTools);
        entityTools.createOperation(new ExportOperation());
        return true;
    }

    @Override
    public boolean start() {
        super.start();
        state = SystemState.OFF;
        li("SmartBoard Agent started");
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
                    case GET_OPERATION:
                        response = handleGetOperation();
                        break;
                    case SET_OPERATION:
                        var targetLuminosity = Double.parseDouble(operationCall.getArgumentValues().get(0).toString());
                        response = handleSetOperation(targetLuminosity);
                        break;
                    case EXPORT_OPERATION:
                        var fileFormat = operationCall.getArgumentValues().get(0).toString();
                        response = handleExportOperation(fileFormat);
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
        if (state == SmartHomeAgent.SystemState.OFF) {
            state = SmartHomeAgent.SystemState.ON;
            return getID() + " The smart board was turned on.";
        }
        return getID() + " The smart board is already on.";
    }

    private String handleTurnOffOperation() {
        if (state == SystemState.ON) {
            state = SystemState.OFF;
            return getID() + " The smart board was turned off.";
        }
        return getID() + " The smart board is already off.";
    }

    private String handleGetOperation() {
        if (state == SystemState.OFF) {
            return getID() + " The smart board is turned off. Turn on the smart board to get the luminosity.";
        }
        return getID() +  " The smart board luminosity is " + luminosity + "%.";
    }

    private String handleSetOperation(double targetLuminosity) {
        if (state == SystemState.OFF) {
            return getID() + " The smart board is turned off. Turn on the smart board to set the luminosity.";
        }
        luminosity = targetLuminosity;
        return getID() + " The smart board luminosity was set to " + targetLuminosity + "%.";
    }

    private String handleExportOperation(String fileFormat) {
        if (state == SystemState.OFF) {
            return getID() + " The smart board is turned off. Turn on the smart board to export a file.";
        }
        return getID() + " The current screen was exported as a " + fileFormat + " file.";
    }
}

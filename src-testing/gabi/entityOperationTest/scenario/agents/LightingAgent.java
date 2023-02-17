package gabi.entityOperationTest.scenario.agents;

import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import static gabi.entityOperationTest.scenario.operations.GetOperation.GET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.SetOperation.SET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOffOperation.TURN_OFF_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;

public class LightingAgent extends RemoteSmartHomeAgent {

    private static final double INITIAL_LUMINOSITY = 50;
    private double luminosity;
    private SystemState state;

    @Override
    public boolean start() {
        super.start();
        state = SystemState.OFF;
        luminosity = INITIAL_LUMINOSITY;
        li("Lighting Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        var operation = operationCall.getTargetOperation();
        var sourceEntityId = operationCall.getSourceEntity();

        if (!authorize(sourceEntityId)) {
            return "The operation is not authorized. Only teachers who have an activity in the classroom can control the lighting.";
        }

        switch (operation) {
            case TURN_ON_OPERATION:
                return handleTurnOnOperation();
            case TURN_OFF_OPERATION:
                return handleTurnOffOperation();
            case GET_OPERATION:
                return handleGetLuminosityOperation();
            case SET_OPERATION:
                var targetLuminosity = Double.parseDouble(operationCall.getArgumentValues().get(0).toString());
                return handleSetLuminosityOperation(targetLuminosity);
        }

        return operation + " operation is not supported by the " + getUnitName() + "entity.";
    }

    private String handleTurnOnOperation() {
        if (state == SystemState.OFF) {
            state = SystemState.ON;
            return getID() + " The lighting was turned on.";
        }
        return getID() + " The lighting is already on.";
    }

    private String handleTurnOffOperation() {
        if (state == SystemState.ON) {
            state = SystemState.OFF;
            return getID() + " The lighting was turned off.";
        }
        return getID() + " The lighting is already off.";
    }

    private String handleGetLuminosityOperation() {
        return getID() + " The luminosity is " + luminosity + "%.";
    }

    private String handleSetLuminosityOperation(double targetLuminosity) {
        if (state == SystemState.OFF) {
            return getID() + " The lighting is turned off. Turn on the lighting to set the luminosity.";
        }
        luminosity = targetLuminosity;
        return getID() + " The luminosity was set to " + targetLuminosity + "%.";
    }

}

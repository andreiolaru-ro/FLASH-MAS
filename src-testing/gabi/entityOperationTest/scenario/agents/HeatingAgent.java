package gabi.entityOperationTest.scenario.agents;

import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import static gabi.entityOperationTest.scenario.operations.GetOperation.GET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.SetOperation.SET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOffOperation.TURN_OFF_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;

public class HeatingAgent extends RemoteSmartHomeAgent {

    private static final double INITIAL_TEMPERATURE = 15;
    private double temperature;
    private SystemState state;

    @Override
    public boolean start() {
        super.start();
        state = SystemState.OFF;
        temperature = INITIAL_TEMPERATURE;
        li("Heating Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        var operation = operationCall.getTargetOperation();
        var sourceEntityId = operationCall.getSourceEntity();

        if (!authorize(sourceEntityId)) {
            return "The operation is not authorized. Only teachers who have an activity in the classroom can control the heating.";
        }

        switch (operation) {
            case TURN_ON_OPERATION:
                return handleTurnOnOperation();
            case TURN_OFF_OPERATION:
                return handleTurnOffOperation();
            case GET_OPERATION:
                return handleGetTemperatureOperation();
            case SET_OPERATION:
                var targetTemperature = Double.parseDouble(operationCall.getArgumentValues().get(0).toString());
                return handleSetTemperatureOperation(targetTemperature);
        }

        return operation + " operation is not supported by the " + getUnitName() + "entity.";
    }

    private String handleTurnOnOperation() {
        if (state == SystemState.OFF) {
            state = SystemState.ON;
            return getID() + " The heating was turned on.";
        }
        return getID() + " The heating is already on.";
    }

    private String handleTurnOffOperation() {
        if (state == SystemState.ON) {
            state = SystemState.OFF;
            return getID() + " The heating was turned off.";
        }
        return getID() + " The heating is already off.";
    }

    private String handleGetTemperatureOperation() {
        return getID() + " The temperature is " + temperature + " degrees Celsius.";
    }

    private String handleSetTemperatureOperation(double targetTemperature) {
        if (state == SystemState.OFF) {
            return getID() + " The heating is turned off. Turn on the heating to set the temperature.";
        }
        temperature = targetTemperature;
        return getID() + " The temperature was set to " + targetTemperature + " degrees Celsius.";
    }

}

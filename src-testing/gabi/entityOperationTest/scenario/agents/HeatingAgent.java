package gabi.entityOperationTest.scenario.agents;

import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Relation;

import static gabi.entityOperationTest.scenario.operations.GetOperation.GET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.SetOperation.SET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOffOperation.TURN_OFF_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.TEACHER;

public class HeatingAgent extends SmartHomeAgent {

    private static final double INITIAL_TEMPERATURE = 18.5;
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
            return "The operation is not authorized. Only teachers can control the heating.";
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
            default:
                le("[] operation is not supported by the [] entity.", operation, getUnitName());
        }
        return null;
    }

    private Object handleTurnOnOperation() {
        if (state == SystemState.OFF) {
            state = SystemState.ON;
            return getID() + " heating was turned on.";
        }
        return getID() + " heating is already on.";
    }

    private Object handleTurnOffOperation() {
        if (state == SystemState.ON) {
            state = SystemState.OFF;
            return getID() + " heating was turned off.";
        }
        return getID() + " heating is already off.";
    }

    private Object handleGetTemperatureOperation() {
        return getID() + " temperature is " + temperature + " degrees Celsius";
    }

    private Object handleSetTemperatureOperation(double targetTemperature) {
        if (state == SystemState.ON) {
            temperature = targetTemperature;
            return getID() + " temperature was set to " + targetTemperature + " degrees Celsius";
        }
        return getID() + " heating is turned off. Turn on the heating to set temperature.";
    }

    private boolean authorize(EntityID sourceEntityId) {
        var relation = new Relation(sourceEntityId, getID(), TEACHER.name());
        return framework.getIncomingRelations().contains(relation);
    }

}

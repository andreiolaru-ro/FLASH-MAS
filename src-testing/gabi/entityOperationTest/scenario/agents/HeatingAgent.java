package gabi.entityOperationTest.scenario.agents;

import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import static gabi.entityOperationTest.scenario.operations.GetOperation.GET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.SetOperation.SET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOffOperation.TURN_OFF_OPERATION;
import static gabi.entityOperationTest.scenario.operations.TurnOnOperation.TURN_ON_OPERATION;

public class HeatingAgent extends SmartHomeAgent {

    private double INITIAL_TEMPERATURE = 18.5;
    private double temperature;
    private SystemState state;

    @Override
    public boolean start() {
        super.start();
        li("Heating Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        var operation = operationCall.getTargetOperation();
        switch (operation) {
            case TURN_ON_OPERATION:
                state = (state == SystemState.ON) ? state : SystemState.OFF;
                break;
            case TURN_OFF_OPERATION:
                break;
            case GET_OPERATION:
                break;
            case SET_OPERATION:
                break;
            default:
                le("[] operation is not supported by the [] entity.", operation, getUnitName());
        }
        return null;
    }
}

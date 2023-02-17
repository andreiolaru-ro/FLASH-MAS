package gabi.entityOperationTest.scenario.agents;

import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import static gabi.entityOperationTest.scenario.operations.CloseOperation.CLOSE_OPERATION;
import static gabi.entityOperationTest.scenario.operations.GetOperation.GET_OPERATION;
import static gabi.entityOperationTest.scenario.operations.OpenOperation.OPEN_OPERATION;

public class DoorAgent extends RemoteSmartHomeAgent {

    private SystemState doorState;

    @Override
    public boolean start() {
        super.start();
        doorState = SystemState.CLOSED;
        li("Door Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        var operation = operationCall.getTargetOperation();
        var sourceEntityId = operationCall.getSourceEntity();

        if (!authorize(sourceEntityId)) {
            return "The operation is not authorized. Only teachers who have an activity in the classroom can open or close the door.";
        }

        switch (operation) {
            case OPEN_OPERATION:
                return handleOpenOperation();
            case CLOSE_OPERATION:
                return handleCloseOperation();
            case GET_OPERATION:
                return handleGetDoorStateOperation();
        }

        return operation + " operation is not supported by the " + getUnitName() + "entity.";
    }

    private String handleOpenOperation() {
        if (doorState == SystemState.CLOSED) {
            doorState = SystemState.OPEN;
            return getID() + " The door was opened.";
        }
        return getID() + " The door is already open.";
    }

    private String handleCloseOperation() {
        if (doorState == SystemState.OPEN) {
            doorState = SystemState.CLOSED;
            return getID() + " The door was closed.";
        }
        return getID() + " The door is already closed.";
    }

    private String handleGetDoorStateOperation() {
        return getID() + " The door is " + doorState.toString();
    }

}

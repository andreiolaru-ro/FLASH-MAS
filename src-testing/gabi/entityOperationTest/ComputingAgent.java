package gabi.entityOperationTest;

import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.operations.MultiplyOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;

import static net.xqhs.flash.ent_op.impl.operations.MultiplyOperation.MULTIPLY_OPERATION;

public class ComputingAgent extends Agent {
    @Override
    public boolean connectTools(OutboundEntityTools entityTools) {
        super.connectTools(entityTools);
        var multiplyOp = new MultiplyOperation();
        framework.createOperation(multiplyOp);
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        super.handleIncomingOperationCall(operationCall);
        var result = 0d;

        if (operationCall.getTargetOperation().equals(MULTIPLY_OPERATION)) {
            result = operationCall.getArgumentValues().stream()
                    .filter(x -> x instanceof Number)
                    .map(Object::toString)
                    .map(Double::valueOf)
                    .reduce(1d, (x, y) -> x * y);
        }
        return result;
    }
}

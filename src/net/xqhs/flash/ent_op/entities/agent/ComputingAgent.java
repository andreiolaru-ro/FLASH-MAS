package net.xqhs.flash.ent_op.entities.agent;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.operations.MultiplyOperation;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.OperationCall;

import static net.xqhs.flash.ent_op.entities.operations.MultiplyOperation.MULTIPLY_OPERATION_NAME;

public class ComputingAgent extends Agent {

    public ComputingAgent() {

    }

    public ComputingAgent(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean setup(MultiTreeMap agentConfiguration) {
        super.setup(agentConfiguration);
        var multiplyOp = new MultiplyOperation();
        entityTools.createOperation(multiplyOp);
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCall operationCall) {
        super.handleIncomingOperationCall(operationCall);
        var result = 0d;

        if (operationCall.getTargetOperation().equals(MULTIPLY_OPERATION_NAME)) {
            result = operationCall.getArgumentValues().stream()
                    .filter(x -> x instanceof Double)
                    .map(x -> (Double) x)
                    .reduce(1d, (x, y) -> x * y);
        }
        return result;
    }
}

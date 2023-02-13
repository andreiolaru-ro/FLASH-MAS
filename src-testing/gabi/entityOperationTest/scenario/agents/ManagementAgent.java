package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.SimpleAgent;
import gabi.entityOperationTest.scenario.operations.AuthOperation;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;

public class ManagementAgent extends SimpleAgent {

    @Override
    public boolean connectTools(OutboundEntityTools entityTools) {
        super.connectTools(entityTools);
        entityTools.createOperation(new AuthOperation());
        return true;
    }

    @Override
    public boolean start() {
        super.start();
        li("Management Agent started");
        return true;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCall) {
        return true;
    }
}

package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.ReceiveOperation.RECEIVE_OPERATION;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class AgentTest {
    public static void main(String[] args) {
        // create fMas
        var fMas = new DefaultFMasImpl();

        // agentA setup
        var agentA = new SimpleAgent();
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentA")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
        fMas.registerEntity(agentA);
        agentA.start();

        // agentB setup
        var agentB = new SimpleAgent();
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
        fMas.registerEntity(agentB);
        agentB.start();

        // agentA calls an agentB operation
        var operationCall = new OperationCallWave(agentA.getID(), agentB.getID(), RECEIVE_OPERATION,
                false, List.of("simple message"));
        agentA.callOperation(operationCall);
    }
}

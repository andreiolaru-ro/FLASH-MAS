package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.MultiplyOperation.MULTIPLY_OPERATION;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class OperationCallWithResultTest {

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
        var agentB = new ComputingAgent();
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
        fMas.registerEntity(agentB);
        agentB.start();

        // agentA calls an agentB operation which returns a result
        var operationCall = new OperationCallWave(agentA.getID(), agentB.getID(), MULTIPLY_OPERATION, true, List.of(1.4d, 8.2d, 3.5d));
        agentA.callOperationWithResult(operationCall, x -> System.out.println("The result of the multiply operation is " + x));
    }
}

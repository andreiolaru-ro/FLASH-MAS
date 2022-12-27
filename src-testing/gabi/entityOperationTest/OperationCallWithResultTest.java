package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.MultiplyOperation.MULTIPLY_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class OperationCallWithResultTest {

    public static void main(String[] args) {
        var fMas = new DefaultFMasImpl();
        var agentA = new Agent(fMas);
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentA")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
        agentA.start();

        var agentB = new ComputingAgent(fMas);
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
        agentB.start();

        List<Object> argumentValues = List.of(1.4d, 8.2d, 3.5d);
        var operationCall = new OperationCallWave(agentA.getEntityID(), agentB.getEntityID(), MULTIPLY_OPERATION_NAME, true, argumentValues);
        agentA.callOperationWithResult(operationCall, System.out::println);
    }
}

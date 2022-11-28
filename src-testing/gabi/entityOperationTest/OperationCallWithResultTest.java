package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.agent.Agent;
import net.xqhs.flash.ent_op.entities.agent.ComputingAgent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.OperationCallWave;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.operations.MultiplyOperation.MULTIPLY_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class OperationCallWithResultTest {

    public static void main(String[] args) {
        FMas fMas = new DefaultFMasImpl();
        Agent agentA = new Agent(fMas);
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentA")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
        agentA.start();

        Agent agentB = new ComputingAgent(fMas);
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
        agentB.start();

        List<Object> argumentValues = List.of(1.4d, 8.2d, 3.5d);
        OperationCallWave operationCall = new OperationCallWave(agentA.getEntityID(), agentB.getEntityID(), MULTIPLY_OPERATION_NAME, true, argumentValues);
        agentA.callOperation(operationCall);
    }
}

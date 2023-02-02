package gabi.entityOperationTest;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.MultiplyOperation.MULTIPLY_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

import java.util.List;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.impl.websocket.WebSocketPylon;

public class OperationCallWithResultTest {

    public static void main(String[] args) {
        var fMas = new DefaultFMasImpl();
		fMas.registerEntity(new WebSocketPylon());
		
		var agentA = new Agent();
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentA")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
		fMas.registerEntity(agentA);
        agentA.start();

		var agentB = new ComputingAgent();
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
		fMas.registerEntity(agentB);
        agentB.start();

        List<Object> argumentValues = List.of(1.4d, 8.2d, 3.5d);
        var operationCall = new OperationCallWave(agentA.getID(), agentB.getID(), MULTIPLY_OPERATION_NAME, true, argumentValues);
        agentA.callOperationWithResult(operationCall, System.out::println);
    }
}

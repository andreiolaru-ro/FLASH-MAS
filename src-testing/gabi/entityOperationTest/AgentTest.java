package gabi.entityOperationTest;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.impl.operations.ReceiveOperation.RECEIVE_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

import java.util.ArrayList;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.FMas;

public class AgentTest {
	public static void main(String[] args) {
		FMas fMas = new DefaultFMasImpl();
		
		Agent agentA = new Agent();
		agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentA")
				.addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
		fMas.registerEntity(agentA);
		
		Agent agentB = new Agent();
		agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
				.addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
		fMas.registerEntity(agentB);
		
		agentA.start();
		agentB.start();
		
		ArrayList<Object> argumentValues = new ArrayList<>();
		argumentValues.add("simple message");
		OperationCallWave operationCall = new OperationCallWave(agentA.getID(), agentB.getID(), RECEIVE_OPERATION_NAME,
				false, argumentValues);
		agentA.callOperation(operationCall);
	}
}

package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.OperationCall;

import java.util.ArrayList;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.ReceiveOperation.RECEIVE_OPERATION_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class AgentTest {
    public static void main(String[] args) {
        FMas fMas = new DefaultFMasImpl();
        Agent agentA = new Agent(fMas);
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentA")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
        agentA.start();

        Agent agentB = new Agent(fMas);
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
        agentB.start();

        ArrayList<Object> argumentValues = new ArrayList<>();
        argumentValues.add("simple message");
        OperationCall operationCall = new OperationCall(agentA.getEntityID(), agentB.getEntityID(), RECEIVE_OPERATION_NAME, false, argumentValues);
        agentA.callOperation(operationCall);
    }
}

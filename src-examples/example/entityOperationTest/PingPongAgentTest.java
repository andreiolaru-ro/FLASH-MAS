package example.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.PingPongAgent;
import net.xqhs.flash.ent_op.entities.PingPongOperation;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.PingPongAgent.DEST_AGENT_PARAMETER_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class PingPongAgentTest {
    public static void main(String[] args) {
        Operation pingPong = new PingPongOperation();
        PingPongAgent agentA = new PingPongAgent();
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "Agent A")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/ping-pong/agentA")
                .addAll(DEST_AGENT_PARAMETER_NAME, List.of("test/ping-pong/agentB", "test/ping-pong/agentC")));
        agentA.start();
        agentA.getEntityTools().createOperation(pingPong);

        PingPongAgent agentB = new PingPongAgent();
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "Agent B")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/ping-pong/agentB"));
        agentB.start();
        agentB.getEntityTools().createOperation(pingPong);

        PingPongAgent agentC = new PingPongAgent();
        agentC.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "Agent C")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/ping-pong/agentC"));
        agentC.start();
        agentC.getEntityTools().createOperation(pingPong);
    }
}

package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.PingPongAgent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.FMas;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.entities.PingPongAgent.DEST_AGENT_PARAMETER_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class PingPongAgentTest {
    public static void main(String[] args) {
        FMas fMas = new DefaultFMasImpl();

        PingPongAgent agentA = new PingPongAgent(fMas);
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/ping-pong/agentA")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/ping-pong/agentA")
                .addAll(DEST_AGENT_PARAMETER_NAME, List.of("test/ping-pong/agentB")));
        agentA.start();

        PingPongAgent agentB = new PingPongAgent(fMas);
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/ping-pong/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/ping-pong/agentB"));
        agentB.start();
    }
}
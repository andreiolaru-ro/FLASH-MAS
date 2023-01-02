package gabi.entityOperationTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.Relation;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class RelationChangeTest {

    public static void main(String[] args) throws JsonProcessingException {
        var fMas = new DefaultFMasImpl();
        var agentA = new Agent(fMas);
        agentA.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentA")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
        agentA.start();

        var agentB = new ComputingAgent(fMas);
        agentB.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/agent/agentB")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
        agentB.start();

        var relation = new Relation(agentA.getEntityID(), agentB.getEntityID(), "agentA-agentB-relation");
        agentA.callRelationChange(Relation.RelationChangeType.CREATE, relation);
        System.out.println(fMas.getEntityTools("test/agent/agentB").getRelations());
        System.out.println(fMas.getEntityTools("test/agent/agentA").getRelations());
        System.out.println(fMas.getEntityTools("test/agent/agentB").getIncomingRelations());
    }
}

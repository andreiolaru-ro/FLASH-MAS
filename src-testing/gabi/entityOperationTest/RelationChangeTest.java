package gabi.entityOperationTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import gabi.entityOperationTest.scenario.agents.SimpleAgent;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;

import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class RelationChangeTest {

    public static void main(String[] args) throws JsonProcessingException {
        // create fMas
        var fMas = new DefaultFMasImpl();

        // agentA setup
        var agentA = new SimpleAgent();
        agentA.setup(new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
        fMas.registerEntity(agentA);
        agentA.start();

        // agentB setup
        var agentB = new ComputingAgent();
        agentB.setup(new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
        fMas.registerEntity(agentB);
        agentB.start();

        // relation change requests
        var relation = new Relation(agentA.getID(), agentB.getID(), "agentA-agentB-relation");
        agentA.callRelationChange(Relation.RelationChangeType.CREATE, relation);
        System.out.println(((OutboundEntityTools) fMas.getEntityTools("test/agent/agentB")).getRelations());
        System.out.println(((OutboundEntityTools) fMas.getEntityTools("test/agent/agentA")).getRelations());

        agentA.callRelationChange(Relation.RelationChangeType.REMOVE, relation);
        System.out.println(((OutboundEntityTools) fMas.getEntityTools("test/agent/agentB")).getRelations());
        System.out.println(((OutboundEntityTools) fMas.getEntityTools("test/agent/agentA")).getRelations());
    }
}

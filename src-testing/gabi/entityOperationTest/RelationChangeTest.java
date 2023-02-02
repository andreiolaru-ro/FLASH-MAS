package gabi.entityOperationTest;

import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

import com.fasterxml.jackson.core.JsonProcessingException;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;
import net.xqhs.flash.ent_op.model.Relation;

public class RelationChangeTest {

    public static void main(String[] args) throws JsonProcessingException {
        var fMas = new DefaultFMasImpl();
		var agentA = new Agent();
		agentA.setup(new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentA"));
		fMas.registerEntity(agentA);
		
		var agentB = new ComputingAgent();
		agentB.setup(new MultiTreeMap().addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/agent/agentB"));
		fMas.registerEntity(agentB);
		
        agentA.start();
        agentB.start();

        var relation = new Relation(agentA.getID(), agentB.getID(), "agentA-agentB-relation");
        agentA.callRelationChange(Relation.RelationChangeType.CREATE, relation);
		System.out.println(((OutboundEntityTools) fMas.getEntityTools("test/agent/agentB")).getRelations());
		System.out.println(((OutboundEntityTools) fMas.getEntityTools("test/agent/agentA")).getRelations());
		System.out.println(((OutboundEntityTools) fMas.getEntityTools("test/agent/agentB")).getIncomingRelations());
    }
}

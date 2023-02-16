package gabi.entityOperationTest.scenario.agents;

import net.xqhs.flash.ent_op.model.EntityID;

import static gabi.entityOperationTest.scenario.relations.ActivityRelation.getAllActivities;
import static gabi.entityOperationTest.scenario.relations.PrecisRelation.TEACHER;

public class RemoteSmartHomeAgent extends SmartHomeAgent {

    public boolean authorize(EntityID sourceEntityId) {
        var hasActivity = framework.getIncomingRelations().stream()
                .filter(relation -> relation.getFrom().equals(sourceEntityId))
                .anyMatch(relation -> getAllActivities().contains(relation.getRelationName()));
        var isTeacher = framework.getIncomingRelations().stream()
                .anyMatch(relation -> relation.getFrom().equals(sourceEntityId) && TEACHER.name().equals(relation.getRelationName()));
        return hasActivity && isTeacher;
    }
}

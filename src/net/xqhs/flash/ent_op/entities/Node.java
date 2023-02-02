package net.xqhs.flash.ent_op.entities;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.Relation.RelationChangeType;

public class Node extends EntityCore implements EntityAPI {

	public static final String EXECUTES_ON_RELATION = "EXECUTES-ON";
	
    /**
     * All added entities.
     */
	protected Map<EntityID, EntityAPI> entities = new HashMap<>();

    @Override
    public boolean start() {
		super.start();
		entities.forEach((key, entity) -> {
			entity.start();
		});
        return true;
    }

	public boolean addEntity(EntityAPI entityAPI) {
		entities.put(entityAPI.getID(), entityAPI);
		framework.changeRelation(RelationChangeType.CREATE,
				new Relation(this.getID(), entityAPI.getID(), EXECUTES_ON_RELATION));
		return true;
    }
}

package net.xqhs.flash.ent_op.impl.waves;

import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.Wave;

import java.io.Serializable;

import static net.xqhs.flash.ent_op.model.Wave.WaveType.RELATION_CHANGE_RESULT;

public class RelationChangeResultWave extends Wave implements Serializable {

    private Relation relation;
    private Relation.RelationChangeResult result;

    public RelationChangeResultWave() {

    }

    public RelationChangeResultWave(EntityID sourceEntity, EntityID targetEntity, Relation relation, Relation.RelationChangeResult result) {
        this.type = RELATION_CHANGE_RESULT;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.relation = relation;
        this.result = result;
    }

    public Relation getRelation() {
        return relation;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public Relation.RelationChangeResult getResult() {
        return result;
    }

    public void setResult(Relation.RelationChangeResult result) {
        this.result = result;
    }
}

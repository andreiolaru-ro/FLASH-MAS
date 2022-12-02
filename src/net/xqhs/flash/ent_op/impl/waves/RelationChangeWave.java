package net.xqhs.flash.ent_op.impl.waves;

import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.flash.ent_op.model.Wave;

import java.io.Serializable;

import static net.xqhs.flash.ent_op.model.Wave.WaveType.RELATION_CHANGE;

public class RelationChangeWave extends Wave implements Serializable {

    protected Relation relation;
    protected Relation.RelationChangeType changeType;
    public RelationChangeWave() {

    }

    public RelationChangeWave(Relation.RelationChangeType changeType, Relation relation) {
        this.type = RELATION_CHANGE;
        this.relation = relation;
        this.changeType = changeType;
        this.sourceEntity = relation.getFrom();
        this.targetEntity = relation.getTo();
    }

    public Relation getRelation() {
        return relation;
    }

    public void setRelation(Relation relation) {
        this.relation = relation;
    }

    public Relation.RelationChangeType getChangeType() {
        return changeType;
    }

    public void setChangeType(Relation.RelationChangeType changeType) {
        this.changeType = changeType;
    }
}

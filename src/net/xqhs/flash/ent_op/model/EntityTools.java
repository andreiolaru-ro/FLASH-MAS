package net.xqhs.flash.ent_op.model;

public interface EntityTools extends InboundEntityTools, OutboundEntityTools {

    boolean createRelation(Relation relation);

    boolean removeRelation(Relation relation);

}

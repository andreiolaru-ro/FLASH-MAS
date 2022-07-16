package net.xqhs.flash.ent_op.entities;


import net.xqhs.flash.ent_op.model.EntityAPI;

public interface Pylon extends EntityAPI {

    boolean canRouteOpCall(String destinationTarget);
}

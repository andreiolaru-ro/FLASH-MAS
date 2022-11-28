package net.xqhs.flash.ent_op.model;

public interface LocalRouter extends EntityAPI {

    /**
     * Routes a wave based on the target entity.
     *
     * @param wave
     *          - the wave that must be routed.
     */
    void route(Wave wave);
}

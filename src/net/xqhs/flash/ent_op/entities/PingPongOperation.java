package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.Set;

public class PingPongOperation implements Operation {
    /**
     * The name of the ping pong operation.
     */
    public static final String PING_PONG_OPERATION_NAME = "PING-PONG";

    @Override
    public String getName() {
        return PING_PONG_OPERATION_NAME;
    }

    @Override
    public Description getDescription() {
        return null;
    }

    @Override
    public boolean hasResult() {
        return false;
    }

    @Override
    public Value getResultType() {
        return null;
    }

    @Override
    public ArrayList<Value> getArguments() {
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

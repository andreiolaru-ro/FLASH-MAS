package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.Set;

public class ReceiveOperation implements Operation {
    /**
     * The name of the operation.
     */
    public static final String RECEIVE_OPERATION_NAME = "RECEIVE";

    @Override
    public String getName() {
        return RECEIVE_OPERATION_NAME;
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

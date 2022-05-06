package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.Set;

public class ReceiveOperation implements Operation {
    /**
     * The name of the operation.
     */
    public static final String RECEIVE_OPERATION_NAME = "RECEIVE";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    public ReceiveOperation() {
        arguments = new ArrayList<>();
        arguments.add(new Value() {
            @Override
            public String getType() {
                return String.class.getName();
            }

            @Override
            public Description getDescription() {
                return () -> "The message of the operation";
            }
        });
    }

    @Override
    public String getName() {
        return RECEIVE_OPERATION_NAME;
    }

    @Override
    public Description getDescription() {
        return () -> "The default operation supported by an agent. This operation takes one argument - the message of the operation.";
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
        return arguments;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

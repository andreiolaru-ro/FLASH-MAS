package net.xqhs.flash.ent_op.impl.operations;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.Set;

public class PingPongOperation implements Operation {
    /**
     * The name of the ping pong operation.
     */
    public static final String PING_PONG_OPERATION = "PING-PONG";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    public PingPongOperation() {
        arguments = new ArrayList<>();
        arguments.add(new Value() {
            @Override
            public String getType() {
                return null;
            }

            @Override
            public Description getDescription() {
                return () -> "This operation doesn't have any arguments.";
            }
        });
    }

    @Override
    public String getName() {
        return PING_PONG_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The default operation supported by a ping-pong agent. " +
                "This operation can be called by a ping-pong agent to ping other " +
                "ping pong agents, the latter sending back a reply.";
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

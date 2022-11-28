package net.xqhs.flash.ent_op.impl.operations;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.Set;

public class RouteOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String ROUTE_OPERATION_NAME = "ROUTE";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    public RouteOperation() {
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
        return ROUTE_OPERATION_NAME;
    }

    @Override
    public Description getDescription() {
        return () -> "The route operation supported by an agent.";
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

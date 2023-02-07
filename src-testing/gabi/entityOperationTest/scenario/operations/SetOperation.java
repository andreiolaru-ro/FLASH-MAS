package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.BooleanValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SetOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String SET_OPERATION = "SET";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    @Override
    public String getName() {
        return SET_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The set operation supported by an agent related to smart home features." +
                " Used to set the property of a system (temperature, humidity level, etc).";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new BooleanValue("The result shows whether the system's property was set successfully.");
    }

    @Override
    public List<Value> getArguments() {
        if (arguments != null)
            return arguments;
        arguments = new ArrayList<>();
        arguments.add(new Value() {
            @Override
            public String getType() {
                return Double.class.getName();
            }

            @Override
            public Description getDescription() {
                return () -> "The property value/level.";
            }
        });
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

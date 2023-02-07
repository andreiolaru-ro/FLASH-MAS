package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.DoubleValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;
import java.util.Set;

public class GetOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String GET_OPERATION = "GET";

    @Override
    public String getName() {
        return GET_OPERATION;
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
        return new DoubleValue("The result of the get operation. Shows the actual state of a system.");
    }

    @Override
    public List<Value> getArguments() {
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

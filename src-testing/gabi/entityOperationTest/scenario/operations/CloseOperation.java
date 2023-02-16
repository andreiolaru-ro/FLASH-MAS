package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.StringValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;
import java.util.Set;

public class CloseOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String CLOSE_OPERATION = "CLOSE";

    @Override
    public String getName() {
        return CLOSE_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The close operation supported by an agent. This operation can be used to close different elements (doors, windows, files, etc.).";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new StringValue("The result of the close operation.");
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

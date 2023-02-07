package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.BooleanValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;
import java.util.Set;

public class TurnOffOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String TURN_OFF_OPERATION = "TURN_OFF";

    @Override
    public String getName() {
        return TURN_OFF_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The turn off operation supported by an agent. The system corresponding to the agent is shut down by this operation.";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new BooleanValue("The result of the turn off operation.");
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

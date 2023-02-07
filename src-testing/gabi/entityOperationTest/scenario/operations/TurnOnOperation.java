package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.BooleanValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;
import java.util.Set;

public class TurnOnOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String TURN_ON_OPERATION = "TURN_ON";

    @Override
    public String getName() {
        return TURN_ON_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The turn on operation supported by an agent. The system corresponding to the agent is started by this operation.";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new BooleanValue("The result of the turn on operation.");
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

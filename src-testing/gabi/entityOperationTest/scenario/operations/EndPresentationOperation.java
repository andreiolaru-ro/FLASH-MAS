package gabi.entityOperationTest.scenario.operations;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;
import java.util.Set;

public class EndPresentationOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String END_PRESENTATION_OPERATION = "END_PRESENTATION";

    @Override
    public String getName() {
        return END_PRESENTATION_OPERATION;
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
    public List<Value> getArguments() {
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

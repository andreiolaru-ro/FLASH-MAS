package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.StringValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class StartPresentationOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String START_PRESENTATION_OPERATION = "START_PRESENTATION";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    @Override
    public String getName() {
        return START_PRESENTATION_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The start presentation operation supported by an agent. This operation receives a presentation file path (pptx or pdf file) and opens it.";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new StringValue("The result of the start presentation operation.");
    }

    @Override
    public List<Value> getArguments() {
        if (arguments != null)
            return arguments;
        arguments = new ArrayList<>();
        arguments.add(new Value() {
            @Override
            public String getType() {
                return String.class.getSimpleName();
            }

            @Override
            public Description getDescription() {
                return () -> "The presentation file path.";
            }
        });
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

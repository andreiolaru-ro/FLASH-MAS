package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.StringValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChangeSlideOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String CHANGE_SLIDE_OPERATION = "CHANGE_SLIDE";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    @Override
    public String getName() {
        return CHANGE_SLIDE_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The change slide operation supported by an agent. This operation changes the presentation to the slide received as parameter.";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new StringValue("The result of the change slide operation.");
    }

    @Override
    public List<Value> getArguments() {
        if (arguments != null)
            return arguments;
        arguments = new ArrayList<>();
        arguments.add(new Value() {
            @Override
            public String getType() {
                return Integer.class.getSimpleName();
            }

            @Override
            public Description getDescription() {
                return () -> "The presentation slide number.";
            }
        });
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

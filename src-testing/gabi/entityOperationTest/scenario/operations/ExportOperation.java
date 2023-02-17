package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.StringValue;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ExportOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String EXPORT_OPERATION = "EXPORT";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    @Override
    public String getName() {
        return EXPORT_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "The export operation supported by an agent. This operation can be used to export files in different formats.";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new StringValue("The result of the export operation.");
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
                return () -> "The export file format.";
            }
        });
        return null;
    }
    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

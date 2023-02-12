package gabi.entityOperationTest.scenario.operations;

import gabi.entityOperationTest.scenario.values.BooleanValue;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AuthOperation implements Operation {

    /**
     * The name of the operation.
     */
    public static final String AUTH_OPERATION = "AUTH";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    @Override
    public String getName() {
        return AUTH_OPERATION;
    }

    @Override
    public Description getDescription() {
        return () -> "Used to authorize operations within a classroom. It verifies that the source agent has the " +
                "necessary permissions to execute the desired operation and that it's located inside the classroom. " +
                "Returns true if the operation is authorized and false otherwise.";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new BooleanValue("The result of the auth operation.");
    }

    @Override
    public List<Value> getArguments() {
        if (arguments != null)
            return arguments;
        arguments = new ArrayList<>();
        arguments.add(new Value() {
            @Override
            public String getType() {
                return OperationCallWave.class.getSimpleName();
            }

            @Override
            public Description getDescription() {
                return () -> "The operation call which needs to be authorized.";
            }
        });
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

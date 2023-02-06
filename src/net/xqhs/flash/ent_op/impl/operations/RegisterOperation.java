package net.xqhs.flash.ent_op.impl.operations;

import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class RegisterOperation implements Operation {

    /**
     * The operation to register an entity, for routing entities that need entities to be registered.
     */
    public static final String REGISTER_OPERATION = "REGISTER";

    /**
     * The arguments list.
     */
    protected ArrayList<Value> arguments;

    @Override
    public String getName() {
        return REGISTER_OPERATION;
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
        if (arguments != null)
            return arguments;
        arguments = new ArrayList<>();
        arguments.add(new Value() {
            @Override
            public String getType() {
                return EntityID.class.getName();
            }

            @Override
            public Description getDescription() {
                return () -> "The ID of the entity to register";
            }
        });
        return null;
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }
}

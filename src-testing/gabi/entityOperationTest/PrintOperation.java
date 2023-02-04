package gabi.entityOperationTest;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;
import java.util.Set;

public class PrintOperation implements Operation {

    @Override
    public String getName() {
        return "PRINT";
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


package gabi.entityOperationTest.scenario.values;

import net.xqhs.flash.ent_op.model.Operation;

public class BooleanValue implements Operation.Value {

    private String description;

    public BooleanValue() {

    }

    public BooleanValue(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return Boolean.class.getName();
    }

    @Override
    public Operation.Description getDescription() {
        return () -> description;
    }

}

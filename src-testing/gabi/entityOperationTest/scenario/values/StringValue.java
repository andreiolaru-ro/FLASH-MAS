package gabi.entityOperationTest.scenario.values;

import net.xqhs.flash.ent_op.model.Operation;

public class StringValue implements Operation.Value {

    private String description;

    public StringValue() {

    }

    public StringValue(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return String.class.getSimpleName();
    }

    @Override
    public Operation.Description getDescription() {
        return () -> description;
    }
}

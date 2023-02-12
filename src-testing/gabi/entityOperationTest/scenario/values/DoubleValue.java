package gabi.entityOperationTest.scenario.values;

import net.xqhs.flash.ent_op.model.Operation;

public class DoubleValue implements Operation.Value {

    private String description;

    public DoubleValue() {

    }

    public DoubleValue(String description) {
        this.description = description;
    }

    @Override
    public String getType() {
        return Double.class.getSimpleName();
    }

    @Override
    public Operation.Description getDescription() {
        return () -> description;
    }
}

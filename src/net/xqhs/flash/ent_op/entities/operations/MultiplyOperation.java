package net.xqhs.flash.ent_op.entities.operations;

import net.xqhs.flash.ent_op.model.Operation;

import java.util.List;
import java.util.Set;

public class MultiplyOperation implements Operation {

    public static final String MULTIPLY_OPERATION_NAME = "MULTIPLY";

    @Override
    public String getName() {
        return MULTIPLY_OPERATION_NAME;
    }

    @Override
    public Description getDescription() {
        return () -> "The multiply operation. Receives a list of numbers to be multiplied. " +
                "The result will be returned to the source entity.";
    }

    @Override
    public boolean hasResult() {
        return true;
    }

    @Override
    public Value getResultType() {
        return new DoubleValue();
    }

    @Override
    public List<Value> getArguments() {
        return List.of(new ListValue());
    }

    @Override
    public Set<Restriction> getRestrictions() {
        return null;
    }

    static class DoubleValue implements Value {

        @Override
        public String getType() {
            return Double.class.getName();
        }

        @Override
        public Description getDescription() {
            return () -> "The result of the MULTIPLY operation is a double.";
        }
    }

    static class ListValue implements Value {

        @Override
        public String getType() {
            return List.class.getName();
        }

        @Override
        public Description getDescription() {
            return () -> "The argument of the MULTIPLY operation is a list of double values.";
        }
    }

}

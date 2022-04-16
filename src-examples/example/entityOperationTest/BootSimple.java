package example.entityOperationTest;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.entities.TestEntity;

import java.util.*;

import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;


public class BootSimple {
    public static void main(String[] args) {
        TestEntity testEntity1 = new TestEntity();
        testEntity1.setup(new MultiTreeMap().addSingleValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "Test Entity 1")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/entity/1"));
        testEntity1.start();
        testEntity1.getTestEntityTools().createOperation(new Operation() {
            @Override
            public String getName() {
                return "PRINT";
            }

            @Override
            public String getOwner() {
                return "test/entity/1";
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
            public ArrayList<Value> getArguments() {
                return null;
            }

            @Override
            public Set<Restriction> getRestrictions() {
                return null;
            }
        });

        TestEntity testEntity2 = new TestEntity();
        testEntity2.setup(new MultiTreeMap().addSingleValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "Test Entity 2")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/entity/2"));
        testEntity2.start();
        ArrayList<Object> argumentValues = new ArrayList<>();
        argumentValues.add("HELLO");
        testEntity2.callOperation(new OperationCall(testEntity2.getEntityID(), null, "PRINT", false, argumentValues));
    }
}

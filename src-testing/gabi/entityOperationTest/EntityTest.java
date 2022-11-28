package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.entities.TestEntity;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.OperationCallWave;

import java.util.ArrayList;
import java.util.Set;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;


public class EntityTest {
    public static void main(String[] args) {
        FMas fMas = new DefaultFMasImpl();

        TestEntity testEntity1 = new TestEntity(fMas);
        testEntity1.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/entity/1")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/entity/1"));
        testEntity1.start();

        TestEntity testEntity2 = new TestEntity(fMas);
        testEntity2.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/entity/2")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/entity/2"));
        testEntity2.start();

        testEntity1.getTestEntityTools().createOperation(new Operation() {
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
            public ArrayList<Value> getArguments() {
                return null;
            }

            @Override
            public Set<Restriction> getRestrictions() {
                return null;
            }
        });
        ArrayList<Object> argumentValues = new ArrayList<>();
        argumentValues.add("HELLO");
        testEntity2.callOperation(new OperationCallWave(testEntity2.getEntityID(), testEntity1.getEntityID(), "PRINT", false, argumentValues));
        testEntity2.callOperation(new OperationCallWave(testEntity2.getEntityID(), testEntity1.getEntityID(), "MULTIPLY", false, argumentValues));
    }
}

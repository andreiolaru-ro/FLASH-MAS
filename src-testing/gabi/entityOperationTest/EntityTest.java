package gabi.entityOperationTest;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

import java.util.ArrayList;
import java.util.Set;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.Operation;


public class EntityTest {
    public static void main(String[] args) {
        var fMas = new DefaultFMasImpl();

		var testEntity1 = new TestEntity();
        testEntity1.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/entity/1")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/entity/1"));
		fMas.registerEntity(testEntity1);
        testEntity1.start();

		var testEntity2 = new TestEntity();
        testEntity2.setup(new MultiTreeMap().addSingleValue(NAME_ATTRIBUTE_NAME, "test/entity/2")
                .addSingleValue(ENTITY_ID_ATTRIBUTE_NAME, "test/entity/2"));
		fMas.registerEntity(testEntity2);
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
        testEntity2.callOperation(new OperationCallWave(testEntity2.getID(), testEntity1.getID(), "PRINT", false, argumentValues));
        testEntity2.callOperation(new OperationCallWave(testEntity2.getID(), testEntity1.getID(), "MULTIPLY", false, argumentValues));
    }
}

package gabi.entityOperationTest;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.DefaultFMasImpl;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;

import java.util.List;

import static net.xqhs.flash.core.DeploymentConfiguration.NAME_ATTRIBUTE_NAME;
import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;


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

        testEntity2.callOperation(new OperationCallWave(testEntity2.getID(), testEntity1.getID(), "PRINT", false, List.of("HELLO")));
        testEntity2.callOperation(new OperationCallWave(testEntity2.getID(), testEntity1.getID(), "MULTIPLY", false, null));
    }
}

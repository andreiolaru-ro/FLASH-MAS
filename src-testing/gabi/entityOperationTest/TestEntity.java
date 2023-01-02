package gabi.entityOperationTest;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.impl.waves.OperationCallWave;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.EntityTools;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.Operation;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import java.util.List;

import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class TestEntity extends Unit implements EntityAPI {

    /**
     * The default name for instances of this implementation.
     */
    protected static final String DEFAULT_NAME = "Default Test Entity";

    /**
     * Indicates whether the implementation is currently running.
     */
    protected boolean isRunning;

    /**
     * The name of this instance.
     */
    protected String name = DEFAULT_NAME;

    /**
     * The id of this instance.
     */
    protected EntityID entityID;

    /**
     * The corresponding entity tools for this instance.
     */
    protected EntityTools entityTools;

    /**
     * The framework instance.
     */
    protected FMas fMas;

    public TestEntity(FMas fMas) {
        this.fMas = fMas;
    }

    @Override
    public boolean setup(MultiTreeMap configuration) {
        name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
        entityID = new EntityID(configuration.getAValue(ENTITY_ID_ATTRIBUTE_NAME));
        entityTools = fMas.registerEntity(this);
        setUnitName(name);
        return true;
    }

    @Override
    public boolean start() {
        // does nothing, only changes the entity's state
        isRunning = true;
        li("[] started", name);
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Object handleIncomingOperationCall(OperationCallWave operationCallWave) {
        if (!isRunning) {
            le("[] is not running", name);
            return null;
        }
        if ("PRINT".equals(operationCallWave.getTargetOperation())) {
            printMessageOperation(operationCallWave.getArgumentValues().get(0).toString());
        }
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return entityID.ID;
    }

    @Override
    public List<Operation> getOperations() {
        return null;
    }

    @Override
    public boolean canRoute(EntityID entityID) {
        return false;
    }

    public EntityID getEntityID() {
        return entityID;
    }

    public EntityTools getTestEntityTools() {
        return entityTools;
    }

    public void callOperation(OperationCallWave operationCall) {
        entityTools.handleOutgoingWave(operationCall);
    }

    private void printMessageOperation(String message) {
        li("received message: []", message);
    }
}

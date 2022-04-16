package net.xqhs.flash.ent_op.entities;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.implem.DefaultFMasImplementation;
import net.xqhs.flash.ent_op.implem.EntityToolsImplementation;
import net.xqhs.flash.ent_op.model.EntityAPI;
import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

import static net.xqhs.flash.ent_op.model.EntityID.ENTITY_ID_ATTRIBUTE_NAME;

public class TestEntity extends Unit implements EntityAPI {
    /**
     * The default name for instances of this implementation.
     */
    private static final String DEFAULT_NAME = "Default Test Entity";

    /**
     * Indicates whether the implementation is currently running.
     */
    private boolean isRunning = false;

    /**
     * The name of this instance.
     */
    private String name = DEFAULT_NAME;

    /**
     * The id of this instance.
     */
    private EntityID entityID;

    /**
     * The corresponding entity tools for this instance.
     */
    private EntityToolsImplementation testEntityTools;

    @Override
    public boolean setup(MultiTreeMap configuration) {
        name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
        entityID = new EntityID(configuration.getAValue(ENTITY_ID_ATTRIBUTE_NAME));
        testEntityTools = new EntityToolsImplementation();
        testEntityTools.initialize(this);
        DefaultFMasImplementation.getInstance().registerEntity(entityID.ID, testEntityTools);
        this.setUnitName(name);
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
    public Object handleOperationCall(OperationCall opCall) {
        if (!isRunning) {
            le("[] is not running", name);
            return null;
        }
        switch(opCall.getOperationName()) {
            case "PRINT":
                printMessageOperation(opCall.getArgumentValues().get(0).toString());
                break;
            default:
                lw("The [] operation is not supported by the [] entity", opCall.getOperationName(), name);
        }
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    public EntityID getEntityID() {
        return entityID;
    }

    public EntityToolsImplementation getTestEntityTools() {
        return testEntityTools;
    }

    public void callOperation(OperationCall operationCall) {
        testEntityTools.handleOutgoingOperationCall(operationCall);
    }

    private void printMessageOperation(String message) {
        li("Print received message []", message);
    }
}
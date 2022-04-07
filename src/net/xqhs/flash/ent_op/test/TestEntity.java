package net.xqhs.flash.ent_op.test;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ent_op.EntityAPI;
import net.xqhs.flash.ent_op.OperationCall;
import net.xqhs.flash.ent_op.Relation;
import net.xqhs.util.logging.Unit;

public class TestEntity extends Unit implements EntityAPI {
    /**
     * The default name for instances of this implementation.
     */
    protected static final String DEFAULT_NAME = "Default Test Entity";

    /**
     * Indicates whether the implementation is currently running.
     */
    protected boolean isRunning = false;

    /**
     * The name of this instance.
     */
    protected String name = DEFAULT_NAME;

    /**
     * The corresponding entity tools for this instance
     */
    protected TestEntityTools testEntityTools;

    @Override
    public boolean setup(MultiTreeMap configuration) {
        name = configuration.getAValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
        testEntityTools = new TestEntityTools();
        testEntityTools.initialize(name);
        return true;
    }

    @Override
    public boolean start() {
        // does nothing, only changes state.
        isRunning = true;
        lf("[] started", name);
        return true;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public Object handleOperationCall(OperationCall opCall) {
        testEntityTools.handleOutgoingOperationCall(opCall);
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }
}

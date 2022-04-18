package net.xqhs.flash.ent_op.implem;

import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.MultiValueMap;
import net.xqhs.flash.ent_op.model.FMas;
import net.xqhs.flash.ent_op.model.LocalRouter;
import net.xqhs.flash.ent_op.model.OperationCall;
import net.xqhs.flash.ent_op.model.Relation;
import net.xqhs.util.logging.Unit;

public class DefaultLocalRouterImplementation extends Unit implements LocalRouter {

    /**
     * The default name for entity tools instances of this implementation.
     */
    private static final String DEFAULT_LOCAL_ROUTER_NAME = "local router";

    /**
     * The instance of the local router
     */
    private static LocalRouter instance;

    /**
     * The multiValueMap contains the list of available operations.
     * (key, value) -> (entityName, operations supported by that entity)
     */
    private static MultiValueMap operations;

    private DefaultLocalRouterImplementation() {
        // private constructor
    }

    public static LocalRouter getInstance() {
        if (instance == null) {
            instance = new DefaultLocalRouterImplementation();
            operations = new MultiValueMap();
        }
        return instance;
    }

    @Override
    public boolean setup(MultiTreeMap configuration) {
        return false;
    }

    @Override
    public boolean start() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public Object handleOperationCall(OperationCall operationCall) {
        return null;
    }

    @Override
    public boolean handleRelationChange(Relation.RelationChangeType changeType, Relation relation) {
        return false;
    }

    @Override
    public String getName() {
        return DEFAULT_LOCAL_ROUTER_NAME;
    }

    @Override
    public void route(OperationCall operationCall) {
        FMas fMas = DefaultFMasImplementation.getInstance();
        String targetEntityName = operationCall.getTargetEntity().ID;

        // internal routing
        if (fMas.entityExistsOnLocalNode(targetEntityName)) {
            operationCall.setRouted(true);
            fMas.route(operationCall);
        } else { //external routing
            // TODO: send the opCall to the pylon
        }
    }
}

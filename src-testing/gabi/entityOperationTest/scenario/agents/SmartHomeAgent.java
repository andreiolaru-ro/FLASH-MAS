package gabi.entityOperationTest.scenario.agents;

import gabi.entityOperationTest.scenario.operations.GetOperation;
import gabi.entityOperationTest.scenario.operations.SetOperation;
import gabi.entityOperationTest.scenario.operations.TurnOffOperation;
import gabi.entityOperationTest.scenario.operations.TurnOnOperation;
import net.xqhs.flash.ent_op.entities.Agent;
import net.xqhs.flash.ent_op.model.OutboundEntityTools;

public class SmartHomeAgent extends Agent {

    public enum SystemState {
        ON, OFF
    }

    @Override
    public boolean connectTools(OutboundEntityTools entityTools) {
        super.connectTools(entityTools);
        entityTools.createOperation(new TurnOnOperation());
        entityTools.createOperation(new TurnOffOperation());
        entityTools.createOperation(new GetOperation());
        entityTools.createOperation(new SetOperation());
        return true;
    }
}

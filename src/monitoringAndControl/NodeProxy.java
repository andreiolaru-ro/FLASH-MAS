package monitoringAndControl;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.node.Node;

import java.util.List;
import java.util.Map;

public interface NodeProxy extends EntityProxy<Node> {

    /**
     * Requests all entities from a node.
     */
    Map<String, List<Entity<?>>> getEntities();

    /*
     * Request entities registered as Agent type.
     */
    List<Agent> getAgentTypeEntities();

    /*
     * Request entities in order.
     */
    List<Entity<?>> getEntitiesOrder();
}

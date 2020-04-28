package net.xqhs.flash.core.monitoring;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface NodeProxy extends EntityProxy<Node> {


    /**
     * Request all agents available in deployment along with their operations.
     * Note: Operations can be performed by themselves.
     *
     * @return
     *          - a {@link HashMap} with agent name as key and available operations as value
     */
    HashMap<String, List<String>> getAgents();

    /**
     * Get the node in the context of which the entity was added.
     * @param childEntity
     *                  - entity from the context
     * @return
     *                  - the name of the parent node
     */
    String getParentNode(String childEntity);

    /**
     * @return
     *                  - a {@link List} with all nodes deployed in the system
     */
    List<String> getNodes();
}

package net.xqhs.flash.core.node.clientApp;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.List;
import java.util.Map;

public class NodeLoaderDecorator extends NodeLoader {
    private final NodeLoader decoratorLoader;

    public NodeLoaderDecorator(NodeLoader decoratorLoader){
        this.decoratorLoader = decoratorLoader;
    }
    public Node loadNode(MultiTreeMap nodeConfiguration, List<MultiTreeMap> subordinateEntities, String deployemntID){
        li("Decorator: Pre-processing before loading node");
        Node node = decoratorLoader.loadNode(nodeConfiguration, subordinateEntities, deployemntID);
        li("Decorator: Post-processing after loading node");

        return node;
    }


        @Override
        public Entity<?> loadEntity (Node node, MultiTreeMap entityConfig, Map < String, Entity < ? >> loadedEntities){
        li("Decorator: Pre-processing before loading entity");
        Entity<?> entity = decoratorLoader.loadEntity(node, entityConfig, loadedEntities);
        li("Decorator: Post-processing after loading entity");
        return entity;

    }
}

package net.xqhs.flash.core.node;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class EntityLoader {
    private Map<String, Map<String, List<Loader<?>>>> loaders;
    private Loader<?> defaultLoader;
    private Map<String, Entity<?>> loaded;

    public EntityLoader(Map<String, Map<String, List<Loader<?>>>> loaders, Loader<?> defaultLoader, Map<String, Entity<?>> loaded) {
        this.loaders = loaders;
        this.defaultLoader = defaultLoader;
        this.loaded = loaded;
    }

    public void loadEntity(Node node, MultiTreeMap nodeConfiguration, MultiTreeMap entityConfiguration) {
        String name = entityConfiguration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
        String kind = null, id = null, cp = entityConfiguration.get(SimpleLoader.CLASSPATH_KEY);
        String local_id = entityConfiguration.getSingleValue(DeploymentConfiguration.LOCAL_ID_ATTRIBUTE);

        if (name != null && name.contains(":")) {
            kind = name.split(":")[0];
            id = name.split(":", 2)[1];
        }

        if (kind == null || kind.isEmpty()) {
            kind = entityConfiguration.get(DeploymentConfiguration.KIND_ATTRIBUTE_NAME);
        }

        if (id == null || id.isEmpty()) {
            id = entityConfiguration.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
            if (id == null) {
                id = name;
            }
        }

        if (name != null && name.contains(":") && id != null) {
            entityConfiguration.addFirst(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, id);
        }

        List<Loader<?>> loaderList = null;
        String catName = nodeConfiguration.getFirstValue("category");
        if (loaders.containsKey(catName)) {
            if (loaders.get(catName).containsKey(kind)) {
                loaderList = loaders.get(catName).get(kind);
            } else if (loaders.get(catName).containsKey(null)) {
                loaderList = loaders.get(catName).get(null);
            }
        }

        List<Entity.EntityProxy<?>> context = new LinkedList<>();
        if (entityConfiguration.isSimple(DeploymentConfiguration.CONTEXT_ELEMENT_NAME)) {
            for (String contextItem : entityConfiguration.getValues(DeploymentConfiguration.CONTEXT_ELEMENT_NAME)) {
                if (loaded.containsKey(contextItem) && loaded.get(contextItem).asContext() != null) {
                    context.add(loaded.get(contextItem).asContext());
                }
            }
        }
        List<MultiTreeMap> subEntities = DeploymentConfiguration.filterContext(null, local_id);
        if (subEntities == null) {
            subEntities = new ArrayList<>();
        }

        Entity<?> entity = null;
        if (loaderList != null) {
            for (Loader<?> loader : loaderList) {
                if (loader.preload(entityConfiguration, context)) {
                    entity = loader.load(entityConfiguration, context, subEntities);
                }
                if (entity != null) break;
            }
        }

        if (entity == null) {
            cp = Loader.autoFind(null, null, cp, kind, id, catName, null);
            if (cp != null) {
                entityConfiguration.addFirstValue(SimpleLoader.CLASSPATH_KEY, cp);
                if (defaultLoader.preload(entityConfiguration, context)) {
                    entity = defaultLoader.load(entityConfiguration, context, subEntities);
                }
            }
        }

        if (entity != null) {
            loaded.put(local_id, entity);
            node.registerEntity(catName, entity, id);
        }
    }
}

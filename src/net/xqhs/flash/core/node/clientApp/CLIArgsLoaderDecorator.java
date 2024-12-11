package net.xqhs.flash.core.node.clientApp;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.util.ClassFactory;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.Logger;

import java.util.List;

public class CLIArgsLoaderDecorator implements Loader {
    private final Loader<?> wrappedLoader;

    @Override
    public boolean configure(MultiTreeMap configuration, Logger log, ClassFactory classLoader) {
        return false;
    }

    @Override
    public boolean preload(MultiTreeMap configuration) {
        return false;
    }

    @Override
    public Entity<?> load(MultiTreeMap configuration) {
        return null;
    }

    @Override
    public Entity<?> load(MultiTreeMap configuration, List context, List subordinateEntities) {
        return null;
    }

    @Override
    public boolean preload(MultiTreeMap configuration, List context) {
        return false;
    }

    public CLIArgsLoaderDecorator(Loader<?> loader) {
        this.wrappedLoader = loader;
    }

    public void readCliArgs(String[] args) {

    }

    public Entity<?> load2(MultiTreeMap entityConfig, List<Entity.EntityProxy<?>> context, List<MultiTreeMap> subEntities) {
        return wrappedLoader.load(entityConfig, context, subEntities);
    }
}

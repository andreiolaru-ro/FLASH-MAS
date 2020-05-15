package com.flashmas.lib;

import android.util.Log;

import com.flashmas.lib.gui.AndroidGuiShard;
import com.flashmas.lib.gui.GuiLinkShard;
import com.flashmas.lib.sensors.SensorsAgentShard;

import net.xqhs.flash.android.AndroidClassFactory;
import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.composite.CompositeAgentLoader;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.BaseLogger;
import net.xqhs.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;

import static com.flashmas.lib.sensors.SensorsAgentShard.SENSOR_TYPES_ARRAY_KEY;

public class CompositeAgentBuilder {
    private MultiTreeMap shardsTree = new MultiTreeMap();
    private MultiTreeMap configuration = new MultiTreeMap();
    private CompositeAgentLoader loader = new CompositeAgentLoader();
    private MultiTreeMap loaderConfig = new MultiTreeMap();
    private static int n = 1;
    private String name = null;

    public CompositeAgentBuilder() {
        loaderConfig.add(CategoryName.PACKAGE.s(), "nothingfornow");
    }

    public CompositeAgentBuilder(String name) {
        this();
        this.name = name;
    }

    public CompositeAgentBuilder addSensorShard(List<Integer> sensorTypes) {
        MultiTreeMap sensorShardConfig = new MultiTreeMap();
        if (sensorTypes != null) {
            sensorShardConfig.addAll(SENSOR_TYPES_ARRAY_KEY, listToStringList(sensorTypes));
        }
        sensorShardConfig.addSingleValue(Loader.SimpleLoader.CLASSPATH_KEY, SensorsAgentShard.class.getName());
        shardsTree.addOneTree("sensorShard", sensorShardConfig);
        return this;
    }

    private List<String> listToStringList(List<Integer> integerList) {
        List<String> stringList = new LinkedList<>();
        for (Integer myInt : integerList) {
            stringList.add(String.valueOf(myInt));
        }

        return stringList;
    }

    public CompositeAgentBuilder addGuiShard() {
        MultiTreeMap guiShardConfig = new MultiTreeMap();
        guiShardConfig.addSingleValue(Loader.SimpleLoader.CLASSPATH_KEY, AndroidGuiShard.class.getName());
        shardsTree.addOneTree("guiShard", guiShardConfig);
        return this;
    }

    public CompositeAgent build() {
        if (name == null) {
            name = "CompositeAgent" + n;
            n++;
        }
        configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, name);
        configuration.addSingleTree("shard", shardsTree);

        loader.configure(loaderConfig, getLogger(), new AndroidClassFactory());
        return (CompositeAgent) loader.load(configuration);
    }

    private Logger getLogger() {
        return new BaseLogger() {
            @Override
            public Object lr(Object o, String s, Object... objects) {
                return null;
            }

            @Override
            protected void l(Level level, String s, Object... objects) {
                for (Object o : objects) {
                    Log.d("Composite agents", o.toString());
                }
            }
        };
    }


    public CompositeAgentBuilder addGuiLinkShard(List<String> designations) {
        MultiTreeMap guiLinkShardConfig = new MultiTreeMap();
        guiLinkShardConfig.addSingleValue(Loader.SimpleLoader.CLASSPATH_KEY, GuiLinkShard.class.getName());
        if (designations != null) {
            guiLinkShardConfig.addAll(GuiLinkShard.SHARD_DESIGNATIONS_KEY, designations);
        }
        shardsTree.addOneTree("guiLinkShard", guiLinkShardConfig);
        return this;

    }
}

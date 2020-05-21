package daniel;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.composite.CompositeAgentLoader;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.pc.PCClassFactory;
import net.xqhs.util.logging.BaseLogger;
import net.xqhs.util.logging.Logger;

import java.util.LinkedList;
import java.util.List;

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

    private List<String> listToStringList(List<Integer> integerList) {
        List<String> stringList = new LinkedList<>();
        for (Integer myInt : integerList) {
            stringList.add(String.valueOf(myInt));
        }

        return stringList;
    }

    public CompositeAgentBuilder addSimpleShard(String shardClasspath, String designation) {
        MultiTreeMap shardConfig = new MultiTreeMap();
        shardConfig.addSingleValue(Loader.SimpleLoader.CLASSPATH_KEY, shardClasspath);
        shardsTree.addOneTree(designation, shardConfig);
        return this;
    }

    public CompositeAgent build() {
        if (name == null) {
            name = "CompositeAgent" + n;
            n++;
        }
        configuration.add(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, name);
        configuration.addSingleTree("shard", shardsTree);

        loader.configure(loaderConfig, getLogger(), new PCClassFactory());
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
                System.out.println(s);
            }
        };
    }
}

package interfaceGenerator;

import interfaceGeneratorTest.BuildPageTest;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class AgentGUI implements Agent {
    private static final String name = "AgentGUI";
    private String interfaceConfiguration;

    public AgentGUI(MultiTreeMap configuration) {
        // System.out.println(configuration);
        var guiShard = configuration.getSingleTree("shard");
        // System.out.println(guiShard);
        var guiTrees = guiShard.getTrees("gui");
        // System.out.println(guiTrees.get(0).getTreeKeys());
        // System.out.println(guiTrees.get(0).getSingleTree("config").getHierarchicalNames());
        // System.out.println(guiTrees.get(0).getSingleTree("config").getHierarchicalNames().get(0));
        interfaceConfiguration = guiTrees.get(0).getSingleTree("config").getHierarchicalNames().get(0);
    }

    @Override
    public boolean start() {
        String[] arguments = new String[2];
        if (interfaceConfiguration.indexOf('{') != -1) {
            arguments[0] = BuildPageTest.INLINE;
        } else {
            arguments[0] = BuildPageTest.FILE;
        }
        arguments[1] = interfaceConfiguration;
        try {
            BuildPageTest.main(arguments);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return false;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {
        return false;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return null;
    }
}

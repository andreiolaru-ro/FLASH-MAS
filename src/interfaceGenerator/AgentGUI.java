package interfaceGenerator;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

public class AgentGUI implements Agent {
    private static final String name = "AgentGUI";
    private String interfaceConfiguration;
    private GUIShard guiShard;
    private PylonProxy pylon;

    public AgentGUI(MultiTreeMap configuration) {
        // System.out.println(configuration);
        var guiShard = configuration.getSingleTree("shard");
        // System.out.println(guiShard);
        var guiTrees = guiShard.getTrees("GuiShard");
        // System.out.println(guiTrees.get(0).getTreeKeys());
        // System.out.println(guiTrees.get(0).getSingleTree("config").getHierarchicalNames());
        // System.out.println(guiTrees.get(0).getSingleTree("config").getHierarchicalNames().get(0));
        this.interfaceConfiguration = guiTrees.get(0).getSingleTree("config").getHierarchicalNames().get(0);
        this.guiShard = new GUIShard();
    }

    @Override
    public boolean start() {
        /*
        String[] arguments = new String[2];

        // check if it's not a file
        if (interfaceConfiguration.indexOf('{') != -1) {
            // inline YAML
            arguments[0] = BuildPageTest.INLINE;
        } else {
            // YAML file
            arguments[0] = BuildPageTest.FILE;
        }
        arguments[1] = interfaceConfiguration;
        try {
            System.out.println("Generating page...");
            BuildPageTest.main(arguments);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

         */
        return true;
    }

    @Override
    public boolean stop() {
        return false;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context) {
        pylon = (PylonProxy) context;
        if (pylon != null) {
            guiShard.addGeneralContext(pylon);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context) {
        pylon = (PylonProxy) context;
        if (pylon != null) {
            guiShard.removeGeneralContext(pylon);
            return true;
        }
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

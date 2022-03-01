package example.shadowProtocolDeployment;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.shadowProtocol.AgentShard;
import net.xqhs.flash.shadowProtocol.ShadowPylon;

import java.util.ArrayList;

public class AgentTestBoot {
    /**
     * The agent to use for testing.
     */
    public static class AgentTest implements Agent
    {
        /**
         * Agent name.
         */
        private String					name;
        /**
         * Agent messaging shard.
         */
        private AbstractMessagingShard messagingShard;
        /**
         * The pylon the agent is in the context of.
         */
        private MessagingPylonProxy pylon;

        /**
         * @param name
         *                 the name
         */
        public AgentTest(String name)
        {
            this.name = name;
        }

        @Override
        public boolean start()
        {
            messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
            if(name.contains("Two"))
                messagingShard.sendMessage(messagingShard.getAgentAddress(), "One-localhost:8885", "Hello from the other side!");
            return true;
        }

        public boolean moveToAnotherNode(String node_name) {
            if (messagingShard instanceof AgentShard) {
                ((AgentShard) messagingShard).moveToAnotherPylon(node_name);
                messagingShard = null;
                return true;
            }
            return false;
        }

        @Override
        public boolean stop()
        {
            return true;
        }

        @Override
        public boolean isRunning()
        {
            return true;
        }

        @Override
        public String getName()
        {
            return name;
        }

        @Override
        public boolean addContext(EntityProxy<Pylon> context)
        {
            pylon = (MessagingPylonProxy) context;
            if(messagingShard != null)
            {
                messagingShard.addGeneralContext(pylon);
            }
            return true;
        }

        @Override
        public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
        {
            return true;
        }

        @Override
        public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
        {
            return false;
        }

        @Override
        public boolean removeContext(EntityProxy<Pylon> context)
        {
            pylon = null;
            return true;
        }

        @Override
        public <C extends Entity<Pylon>> EntityProxy<C> asContext()
        {
            return null;
        }

        /**
         * @param shard
         *                  messaging shard to add.
         */
        public void addMessagingShard(AbstractMessagingShard shard)
        {
            messagingShard = shard;
            shard.addContext(new ShardContainer() {
                @Override
                public void postAgentEvent(AgentEvent event)
                {
                    if(event instanceof AgentWave)
                        System.out.println(
                                ((AgentWave) event).getContent() + " de la " + ((AgentWave) event).getCompleteSource()
                                        + " la " + ((AgentWave) event).getCompleteDestination());
                }

                @Override
                public net.xqhs.flash.core.shard.AgentShard getAgentShard(AgentShardDesignation designation)
                {
                    return null;
                }

                @Override
                public String getEntityName()
                {
                    return getName();
                }

            });
            if(pylon != null)
                messagingShard.addGeneralContext(pylon);
        }

        /**
         * @return relay for the messaging shard.
         */
        protected AbstractMessagingShard getMessagingShard()
        {
            return messagingShard;
        }
    }

    /**
     * @param args
     * @throws InterruptedException
     */
    public static void main(String[] args) throws InterruptedException
    {
        ArrayList<String> servers = new ArrayList<>();
        servers.add("ws://localhost:8885");
        servers.add("ws://localhost:8886");

        ShadowPylon pylon = new ShadowPylon();
        pylon.configure(
                new MultiTreeMap().addSingleValue(ShadowPylon.HOME_SERVER_ADDRESS_NAME, "ws://localhost:8885")
                        .addSingleValue(ShadowPylon.HOME_SERVER_PORT_NAME, "8885")
                        .addSingleValue("servers", servers.toString())
                        .addSingleValue("pylon_name", "Pylon-One"));

        pylon.start();
        AgentTest one = new AgentTest("One-" + "localhost:8885");
        one.addContext(pylon.asContext());
        one.addMessagingShard(new AgentShard(pylon.HomeServerAddressName, one.name));


        ShadowPylon pylon2 = new ShadowPylon();
        pylon2.configure(
                new MultiTreeMap().addSingleValue(ShadowPylon.HOME_SERVER_ADDRESS_NAME, "ws://localhost:8886")
                        .addSingleValue(ShadowPylon.HOME_SERVER_PORT_NAME, "8886")
                        .addSingleValue("servers", servers.toString())
                        .addSingleValue("pylon_name", "Pylon-Two"));

        pylon2.start();
        AgentTest two = new AgentTest("Two-" + "localhost:8886");
        two.addContext(pylon2.asContext());
        two.addMessagingShard(new AgentShard("ws://localhost:8886", two.name));

        Thread.sleep(1000);

        one.start();
        two.start();

        Thread.sleep(1000);

        one.moveToAnotherNode("Pylon-Two");

        Thread.sleep(1000);

        one.addContext(pylon2.asContext());
        one.addMessagingShard(new AgentShard(pylon2.HomeServerAddressName, one.name));

        one.start();
        two.start();

        Thread.sleep(3000);

        pylon2.stop();
        pylon.stop();

    }
}

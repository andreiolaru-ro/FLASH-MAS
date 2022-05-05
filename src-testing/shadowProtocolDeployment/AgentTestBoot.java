package shadowProtocolDeployment;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.shadowProtocol.ShadowAgentShard;
import net.xqhs.flash.shadowProtocol.ShadowPylon;

import java.util.List;

public class AgentTestBoot {
    /**
     * The agent to use for testing.
     */
    public static class AgentTest implements Agent
    {
        /**
         * Agent name.
         */
        private final String name;
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
            return true;
        }

        public void sendMessage(String destination, String content) {
            messagingShard.sendMessage(messagingShard.getAgentAddress(), destination, content);
        }

        public void moveToAnotherNode() {
            messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.BEFORE_MOVE));
//            if (messagingShard instanceof ShadowAgentShard) {
//                try {
//                    synchronized (((ShadowAgentShard) messagingShard).lock) {
//                        ((ShadowAgentShard) messagingShard).lock.wait();
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            }
        }

        public void reconnect() {
            messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AFTER_MOVE));
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

    public static void main(String[] args) throws InterruptedException
    {
        TestClass test = new TestClass("src-examples/example/shadowProtocolDeployment/RandomTestCases/Test2.json");
        List<Action> testCase = test.generateTest(10, 5);
        test.CreateElements();
        System.out.println();
        System.out.println();
        test.runTest(testCase);
        System.out.println();
        System.out.println();
        test.closeConnections();
    }
}

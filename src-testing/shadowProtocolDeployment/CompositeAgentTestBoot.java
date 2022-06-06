package shadowProtocolDeployment;

import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.shadowProtocol.ShadowPylon;

import java.io.Serializable;
import java.util.List;


public class CompositeAgentTestBoot {
    /**
     * The agent to use for testing.
     */
    public static class CompositeAgentTest extends CompositeAgent implements Serializable
    {

        protected String agentName;

        public CompositeAgentTest(String name) {
            this.agentName = name;
        }

        public String getName() {
            return this.agentName;
        }

        @Override
        protected AgentShard getShard(AgentShardDesignation designation) {
            return super.getShard(designation);
        }
    }

    public static void main(String[] args) throws InterruptedException
    {
        TestClass test = new TestClass("src-testing/shadowProtocolDeployment/RandomTestCases/Test2.json");
        List<Action> testCase = test.generateTest(10, 0);

        test.CreateElements(testCase);
        test.waitForAgents();
        //test.startTest();
        //Thread.sleep(5000);
        //test.closeConnections();
    }
}

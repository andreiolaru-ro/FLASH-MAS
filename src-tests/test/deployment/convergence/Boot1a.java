package test.deployment.convergence;

import net.xqhs.flash.FlashBoot;

/**
 * Test 1a: node and pylon in XML, agents added via CLI - automatic porting to pylon.
 * <p>
 * Expected behavior: agents are automatically ported to the pylon defined in the XML deployment file.
 * AgentA sends 5 ping messages to AgentB, which replies to each. Both agents stop after the ping limit is reached.
*/

public class Boot1a
{    
    public static void main(String[] args)
    {
        String test_args = "src-tests/test/deployment/convergence/deployment1.xml";
        
        test_args += " -package testing";
        test_args += " -agent PingPong:AgentA classpath:AgentPingPong sendTo:AgentB";
		test_args += " -agent PingPong:AgentB classpath:AgentPingPong";
        
        FlashBoot.main(test_args.split(" "));
    }

}

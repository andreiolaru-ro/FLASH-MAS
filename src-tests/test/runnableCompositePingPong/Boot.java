package test.runnableCompositePingPong;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;

/**
 * Deployment testing.
 */
public class Boot
{
	/**
	 * Stop after this time.
	 */
	public static long RUN_TIME = 5000;
	
	/**
	 * Performs test.
	 * 
	 * @param args
	 *                 - not used.
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException
	{
		String test_args = "";
		
		test_args += " -package test.compositePingPong test.runnableCompositePingPong -loader agent:composite";
		test_args += " -node main classpath:NodeAccess";
		test_args += " -support local:main-pylon use-thread";
		test_args += " -agent composite:AgentA -shard messaging -shard PingTestComponent otherAgent:AgentB -shard MonitoringTest";
		test_args += " -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";
		
		List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(test_args.split(" ")));
		NodeAccess node = (NodeAccess) nodes.get(1); // the 0-th node is node <null>
		// node.start();
		// Thread.sleep(RUN_TIME);
		// node.stop();
		for(Entity<?> e : node.getOtherEntities())
			e.start();
		Set<Thread> threads = new HashSet<>();
		for(Entity<?> e : node.getAgents())
			threads.add(new Thread((Runnable) e));
		for(Thread t : threads)
			t.start();
		Thread.sleep(RUN_TIME);
		for(Entity<?> e : node.getAgents())
			e.stop();
		for(Entity<?> e : node.getOtherEntities())
			e.stop();
		for(Thread t : threads)
			t.join();
		System.out.println("Done.");
	}
	
}

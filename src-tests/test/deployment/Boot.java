package test.deployment;

import net.xqhs.flash.FlashBoot;

/**
 * Runs test XML deployments.
 * 
 * @author Andrei Olaru
 */
public class Boot {
	
	/**
	 * This directory.
	 */
	static String THIS_DIRECTORY = "src-tests/test/empties/";
	
	/**
	 * Main method.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		
		String a = "-package testing -loader agent:composite";
		// merge:
		// a += " -load_order agent;dummy -node node1 -dummy D classpath:DummyEntity -agent composite:A -shard
		// EchoTesting exit:2";
		
		// nu mergecum ne dorim (dummy este pus in contextul agentului):
		a += " -load_order agent;dummy -node node1 -agent composite:A -shard EchoTesting exit:2 -dummy D classpath:DummyEntity";
		
		// am vrea sa mearga (dummy sa fie pus in contextul nodului):
		// a += " -load_order agent;dummy -node node1 -agent composite:A -shard EchoTesting exit:2 <node -dummy D
		// classpath:DummyEntity";
		
		FlashBoot.main(a.split(" "));
	}
	
}

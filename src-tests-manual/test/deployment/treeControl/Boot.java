package test.deployment.treeControl;

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
		// Basic case, no tree control, works fine
		// a += " -load_order agent;dummy -node node1 -dummy D classpath:DummyEntity -agent composite:A -shard EchoTesting exit:2";
		
		// Basic case, no tree control, does not work as expected (dummy is put in the agent's context instead of the node's):
		// a += " -load_order agent;dummy -node node1 -agent composite:A -shard EchoTesting exit:2 -dummy D classpath:DummyEntity";

		// ---  Tree control tests  ---
		// Uncomment one test case at a time and run to check the output

		// Test 1: "<node" (dummy should be put in the node's context):
		// a += " -load_order agent;dummy -node node1 -agent composite:A -shard EchoTesting exit:2 <node -dummy D classpath:DummyEntity";

		// Test 2: "<<" (node2 should be placed directly under the root, and the dummy under node2)
		// a += " -load_order agent;dummy -node node1 -agent composite:A -shard EchoTesting exit:2 << -node node2 -dummy D classpath:DummyEntity";

		// Test 3: "<agent" (dummy should be placed under agent A)
		// a += " -load_order agent;dummy -node node1 -agent composite:A -shard EchoTesting exit:2 <agent -dummy D classpath:DummyEntity";

		// Test 4: Special case / category does not exist in current context (should print a warning message)
		// a += " -load_order agent;dummy -node node1 -agent composite:A -shard EchoTesting exit:2 <group -dummy D classpath:DummyEntity";

		// Test 5: Combination of tree control commands (dummy1 should have node1 as its parent, node2 should be a sibling to node1, also dummy2 should have node2 as its parent)
		// a += " -load_order agent; -node node1 -agent composite:A -shard EchoTesting exit:2 <node -dummy D1 classpath:DummyEntity << -node node2 -dummy D2 classpath:DummyEntity";

		FlashBoot.main(a.split(" "));
	}
	
}

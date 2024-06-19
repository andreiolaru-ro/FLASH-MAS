package test.scriptTestingTest;

import net.xqhs.flash.FlashBoot;

/**
 * @author Andrei Olaru
 */
public class Boot {
	/**
	 * Do testing.
	 * 
	 * @param __args
	 */
	public static void main(String[] __args) {
		String args = "";
		
		args += " -package testing src-tests.test.scriptTestingTest -loader agent:mobileComposite ";
		
		args += " -node nodeA -pylon webSocket:A isServer:localhost:8887";
		args += " -node nodeB -pylon webSocket:B connectTo:ws://localhost:8887";
		args += " -agent agent1 -shard messaging -shard EchoTesting -shard ScriptTesting from:Test1";
		args += " -agent agent2 -shard messaging -shard EchoTesting -shard ScriptTesting from:Test1";
		
		
		FlashBoot.main(args.split(" "));
	}
}

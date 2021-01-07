package andrei.guiWorks;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootGuiAgent
{
	/**
	 * Performs test.
	 * 
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args)
	{
		String test_args = "";

		test_args += " -loader agent:composite";
		test_args += " -package andrei.guiWorks";

		test_args += " -node main";
		test_args += " -agent composite:AgentA -shard messaging -shard control -shard monitoring -shard swingGui from:one-port.yml -shard test";
		test_args += " -agent AgentB -gui from: one-port.yml";
		
		FlashBoot.main(test_args.split(" "));
	}
	
}

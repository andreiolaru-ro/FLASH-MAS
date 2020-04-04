package monitoringAndControl.monitoringAndControlTest;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class Boot
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

//		test_args += " -package  monitoringAndControl.monitoringAndControlTest";
//
//		test_args += " -node main1";
//		test_args += " -agent AgentA classpath:monitoringAndControl.monitoringAndControlTest.AgentTest";
//		test_args += " -agent AgentB classpath:monitoringAndControl.monitoringAndControlTest.AgentTest";
//
//		test_args += " -node main2";
//		test_args += " -agent AgentC classpath:monitoringAndControl.monitoringAndControlTest.AgentTest";

		test_args += " -package monitoringAndControl.monitoringAndControlTest -loader agent:composite";

		test_args += " -node central";
		test_args += " -agent composite:AgentA -shard messaging -shard MonitoringShard";
		test_args += " -agent composite:AgentB -shard messaging -shard MonitoringShard";

		test_args += " -node ordinarynode";
		test_args += " -agent composite:AgentC -shard messaging -shard MonitoringShard";


		FlashBoot.main(test_args.split(" "));
	}
	
}

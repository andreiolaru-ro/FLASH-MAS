package test.remoteManagement;

import net.xqhs.flash.FlashBoot;

/**
 * Tests the deployment of remote nodes via GUI features and the flash demon.
 * 
 * @author andreiolaru
 */
public class BootRemotesLocal {
	/**
	 * Performs test.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		String a = "";
		
		a += " -loader agent:composite";
		a += " -package test.guiGeneration testing";
		
		a += " -node main central:web";
		a += " -pylon webSocket:pylon1 serverPort:8886";
		a += " -agent AgentA -shard messaging -shard remoteOperation -shard PingTest otherAgent:AgentB";
		a += " -node remote1@127.0.0.1";
		a += " -agent AgentB -shard messaging -shard remoteOperation -shard PingBackTest";
		
		FlashBoot.main(a.split(" "));
	}
}

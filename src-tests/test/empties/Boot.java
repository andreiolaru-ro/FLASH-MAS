package test.empties;

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
		// FlashBoot.main(new String[] { THIS_DIRECTORY + "empty-scenario.xml" });
		// FlashBoot.main(new String[] { THIS_DIRECTORY + "empty-nodes.xml" });
		FlashBoot.main(new String[] { THIS_DIRECTORY + "empty-agents.xml" });
	}
	
}

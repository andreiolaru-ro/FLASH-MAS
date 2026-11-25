package test.empties;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.core.DeploymentConfiguration.SourceFileDirectory;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * Runs test XML deployments.
 * 
 * @author Andrei Olaru
 */
public class Boot {
	
	/**
	 * This directory.
	 */
	static String THIS_DIRECTORY = PlatformUtils.buildFilePath(SourceFileDirectory.SRC_TESTS_MANUAL,
			Boot.class) + "/";
	
	/**
	 * Main method.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		// FlashBoot.main(new String[] { THIS_DIRECTORY + "empty-scenario.xml" });
		// FlashBoot.main(new String[] { THIS_DIRECTORY + "empty-nodes.xml" });
		// FlashBoot.main(new String[] { THIS_DIRECTORY + "empty-agents.xml" });
		
		// empty scenario
		// FlashBoot.main(new String[] {});
		// CLI empty node:
		FlashBoot.main("-node node1 -node node2".split(" "));
		// empty scenario
		// FlashBoot.main("-agent -agent".split(" "));
	}
	
}

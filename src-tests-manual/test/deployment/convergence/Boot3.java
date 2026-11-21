package test.deployment.convergence;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.core.DeploymentConfiguration.SourceFileDirectory;
import net.xqhs.flash.core.util.PlatformUtils;

/**
 * Test 3: full deployment in XML, additional parameters added through CLI for same agents.
 * <p>
 * Expected behavior: agents specified in CLI with the same name as agents in XML should be treated
 * as the same agents, with the CLI parameters added to their configuration.
 * <p>
 * Current limitation: the framework creates duplicate agent instances instead of merging the
 * configurations. The ping-number:3 parameter from CLI is not applied to the XML agents, which
 * still send the default 5 ping messages.
*/

public class Boot3
{
    public static void main(String[] args)
	{
		String test_args = PlatformUtils.buildFilePath(SourceFileDirectory.SRC_TESTS_MANUAL,
				                                       Boot3.class, "deployment3.xml");
		
        test_args += " -package testing";
        test_args += " -agent AgentA ping-number:3";
		test_args += " -agent AgentC ping-number:3";
		
		FlashBoot.main(test_args.split(" "));
	}
}

package andrei.partialCLI;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.DeploymentConfiguration.CtxtTriple;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.logging.UnitComponent;

public class PartialCLI_Test {
	
	public static void main(String[] args) {
		String[] argset = "-agent composite:AgentA -shard messaging par:val -shard remoteOperation -agent agentB parameter:one"
				.split(" ");
		MultiTreeMap tree = new MultiTreeMap();
		CtxtTriple ctx = new CtxtTriple(CategoryName.DEPLOYMENT.s(), null, tree);
		DeploymentConfiguration.readCLIArgs(Arrays.asList(argset).iterator(), ctx, tree, new LinkedList<>(),
				new HashMap<>(), new UnitComponent("test"));
		System.out.println(tree);
		System.out.println(ctx.toStringFull());
	}
	
}

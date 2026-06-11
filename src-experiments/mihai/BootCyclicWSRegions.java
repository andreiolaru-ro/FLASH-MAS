package mihai;

import net.xqhs.flash.FlashBoot;

public class BootCyclicWSRegions {

	public static void main(String[] args_) {
		String args = "";
		args += " -package wsRegions testing mihai -loader agent:composite -load_order pylon;agent;bridge";

		args += " -node nodeB keep:50";
		args += " -pylon WSRegions:pylonB isServer:localhost:8886";
		args += " -agent :ws://localhost:8886/agentB -shard messaging -shard EchoTesting";

		args += " -node nodeC keep:50";
		args += " -pylon WSRegions:pylonC isServer:localhost:8887";
		args += " -agent :ws://localhost:8887/agentC -shard messaging -shard EchoTesting";

		args += " -node nodeE keep:50";
		args += " -pylon WSRegions:pylonE isServer:localhost:8888";
		args += " -agent :ws://localhost:8888/agentE -shard messaging -shard EchoTesting";

		args += " -node nodeBC keep:50";
		args += " -pylon WSRegions:pylonBC_B connectTo:localhost:8886";
		args += " -pylon WSRegions:pylonBC_C connectTo:localhost:8887";
		args += " -bridge interoperability:bridgeBC in-context-of:WSRegions:pylonBC_B";

		args += " -node nodeCE keep:50";
		args += " -pylon WSRegions:pylonCE_C connectTo:localhost:8887";
		args += " -pylon WSRegions:pylonCE_E connectTo:localhost:8888";
		args += " -bridge interoperability:bridgeCE in-context-of:WSRegions:pylonCE_C";

		args += " -node nodeEB keep:50";
		args += " -pylon WSRegions:pylonEB_E connectTo:localhost:8888";
		args += " -pylon WSRegions:pylonEB_B connectTo:localhost:8886";
		args += " -bridge interoperability:bridgeEB in-context-of:WSRegions:pylonEB_E";

		FlashBoot.main(args.split(" "));
	}
}
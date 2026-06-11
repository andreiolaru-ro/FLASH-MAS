package mihai;

import net.xqhs.flash.FlashBoot;

public class BootCyclicComplexScenario {

	public static void main(String[] args_) {
		String args = "";

		args += " -package wsRegions testing mihai -loader agent:composite -load_order pylon;agent;bridge";

		// -----------------------------------------------------------------------
		// Infrastructure node A: WSRegions (agentA lives here)
		// agentA pings agentB indefinitely (keep:true) so we can observe
		// messages both before and after the short-path bridge fails.
		// -----------------------------------------------------------------------
		args += " -node nodeA keep:60";
		args += " -pylon WSRegions:pylonA isServer:localhost:8886";
		args += " -agent :ws://localhost:8886/agentA -shard messaging -shard EchoTesting -shard PingTest otherAgent:ws://localhost:8887/agentB every:3000 n:-1 keep";

		// -----------------------------------------------------------------------
		// Infrastructure node B: WebSocket (agentB lives here)
		// agentB echoes all events and sends back a pong for every ping it gets.
		// -----------------------------------------------------------------------
		args += " -node nodeB keep:60";
		args += " -pylon webSocket:pylonB isServer:localhost:8887 serverName:WS-Main";
		args += " -agent agentB -shard messaging -shard EchoTesting";

		// -----------------------------------------------------------------------
		// Infrastructure node C: WSRegions (agentC lives here)
		// agentC acts as an intermediate relay node in the alternate path.
		// -----------------------------------------------------------------------
		args += " -node nodeC keep:60";
		args += " -pylon WSRegions:pylonC isServer:localhost:8888";
		args += " -agent :ws://localhost:8888/agentC -shard messaging -shard EchoTesting";

		// -----------------------------------------------------------------------
		// SHORT PATH bridge node1: WSRegions <-> WebSocket (direct, 1 hop)
		// This node is kept alive for only 15 seconds, simulating a link failure.
		// After it stops, the interoperability router falls back to the alternate path.
		// -----------------------------------------------------------------------
		args += " -node node1 keep:15";
		args += " -pylon WSRegions:pylon1_A connectTo:localhost:8886";
		args += " -pylon webSocket:pylon1_B connectTo:ws://localhost:8887";
		args += " -bridge interoperability:bridge1 in-context-of:WSRegions:pylon1_A";

		// -----------------------------------------------------------------------
		// ALTERNATE PATH – first leg: WSRegions (nodeA) <-> WSRegions (nodeC) (node2)
		// Stays alive for the full duration (keep:60).
		// -----------------------------------------------------------------------
		args += " -node node2 keep:60";
		args += " -pylon WSRegions:pylon2_A connectTo:localhost:8886";
		args += " -pylon WSRegions:pylon2_C connectTo:localhost:8888";
		args += " -bridge interoperability:bridge2 in-context-of:WSRegions:pylon2_A";

		// -----------------------------------------------------------------------
		// ALTERNATE PATH – second leg: WSRegions (nodeC) <-> WebSocket (node3)
		// Stays alive for the full duration (keep:60).
		// -----------------------------------------------------------------------
		args += " -node node3 keep:60";
		args += " -pylon WSRegions:pylon3_C connectTo:localhost:8888";
		args += " -pylon webSocket:pylon3_B connectTo:ws://localhost:8887";
		args += " -bridge interoperability:bridge3 in-context-of:WSRegions:pylon3_C";

		FlashBoot.main(args.split(" "));
	}
}

package marius;

import net.xqhs.flash.FlashBoot;

/**
 * Scenario
 * A1 in WebSocket infrastructure       'ws://A'
 * A3 in WSRegion R1                    'ws://R1'
 * A2 in WSRegion R2                    'ws://R2'
 * A4 in WebSocket infrastructure       'ws://B'
 * Bridges from A to R1 ; R2 to B
 */
public class BootTest {
    /**
     * Performs test
     *
     * @param args_ is not used
      */

    public static void main(String[] args_) {
        String args = "";
        args += " -package wsRegions testing marius -loader agent:composite -load_order pylon;agent;bridge";

        // bridges
		args += " -node nodeBridge_AtoR1 keep:20";
		args += " -pylon webSocket:pylonWebsocket1 isServer:localhost:8886"; // zone A server
		args += " -pylon WSRegions:pylonWSRegions1 isServer:localhost:8885"; // zone R1 server servers:localhost:8887
		args += " -bridge interoperability:bridge1 in-context-of:webSocket:pylonWebsocket1";

		args += " -node nodeBridge_R2toB keep:20";
		// args += " -pylon webSocket:pylonWebsocket2 isServer:localhost:8888"; // zone B server
		args += " -pylon WSRegions:pylonWSRegions2 isServer:localhost:8887 servers:localhost:8885"; // zone R2 server
		// args += " -bridge interoperability:bridge2 in-context-of:webSocket:pylonWebsocket";


        // zone A
		args += " -node node1_A";
        args += " -pylon webSocket:pylonA_1 connectTo:ws://localhost:8886";
		 args += " -agent agentA1 -shard messaging -shard EchoTesting -shard PingTest otherAgent:ws://localhost:8885/agentA3"; // agent
																																// A1->A3
//		args += " -agent agentA1 -shard messaging -shard EchoTesting -shard PingTest otherAgent:ws://localhost:8887/agentA2"; // agent
																																// A1->A2

	    args += " -node node2_A keep:20";
        args += " -pylon webSocket:pylonA_2 connectTo:ws://localhost:8886";

        args += " -node node3_A keep:20";
        args += " -pylon webSocket:pylonA_3 connectTo:ws://localhost:8886";


        // zone R1
		args += " -node node_R1";
        args += " -pylon WSRegions:pylonR1 connectTo:localhost:8885";
		args += " -agent :ws://localhost:8885/agentA3 -shard messaging -shard EchoTesting focus exit:20"; // agent A3


		// // zone R2
		args += " -node node1_R2";
		args += " -pylon WSRegions:pylonR2_1 connectTo:localhost:8887";
		
		args += " -node node2_R2";
		args += " -pylon WSRegions:pylonR2_2 connectTo:localhost:8887";
		args += " -agent :ws://localhost:8887/agentA2 -shard messaging -shard EchoTesting exit:20"; // agent A2


//        // zone B
//        args += " -node node1_B keep:20";
//        args += " -pylon webSocket:pylonB_1 connectTo:ws://localhost:8888";
//        args += " -agent :ws://localhost:8888/agentA4 -shard messaging -shard EchoTesting exit:30"; // agent A4

//
//        args += " -node node2_B keep:20";
//        args += " -pylon webSocket:pylonB_2 connectTo:ws://localhost:8888";


        FlashBoot.main(args.split(" "));
    }
}

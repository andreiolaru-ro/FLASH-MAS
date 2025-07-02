/*******************************************************************************
 * Copyright (C) 2023 Andrei Olaru.
 *
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 *
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 *
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fedlearn;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.fedlearn.Constants;

/**
 * Runs a distributed federated learning scenario.
 */
public class FedBootDistributed {

	public final static String WS_SERVER = "ws://10.40.135.69:";
	public final static String MY_WS_SERVER = "ws://localhost:";
	public static final int WS_SERVER_PORT = 8886;

	/**
	 * Performs the federated learning task.
	 *
	 * @param args
	 * - args[0] should be the hostIndex.
	 * - hostIndex = 0: The server machine. It also runs any clients not designated as 'distributed'.
	 * - hostIndex = 1: The machine for the 1st distributed client.
	 * - hostIndex = 2: The machine for the 2nd distributed client, and so on.
	 */
	public static void main(final String[] args) {
		String a = "";
		int hostIndex = args.length > 0 ? Integer.parseInt(args[0]) : 0;

		// Total number of fl clients in the experiment.
		int nclients = 2;

		// The number of clients that will run on dedicated, separate machines.
		int num_distributed_clients = 2;

		a += " -load_order driver;pylon;agent -loader agent:composite";
		a += " -package testing net.xqhs.flash.fedlearn " + FedBoot.class.getPackageName();

		// If hostIndex is 0, configure and run the server node.
		if(hostIndex == 0) {
			a += " -node nodeServer keep -driver Fed:FedDriver port:8080";
			a += " -pylon webSocket:pylon0 serverPort:" + WS_SERVER_PORT;
			// Server agent configuration
			a += " -agent agentS  in-context-of:Fed:FedDriver";
			a += " -shard messaging";
			a += " -shard FedServer" +
					" nclients:" + nclients +
					" fraction_fit:1.0" +
					" fraction_evaluate:1.0" +
					" min_fit_clients:" + nclients +
					" min_evaluate_clients:" + nclients +
					" min_available_clients:" + nclients +
					" num_rounds:3" +
					" timeout:240.0";
		}

		int BASE_CLIENT_PORT = 8090;
		for(int client = 1; client <= nclients; client++) {

			// DISTRIBUTED CLIENTS
			if (hostIndex > 0 && client == hostIndex && client <= num_distributed_clients) {
				a += " -node nodeClient" + client + " keep -driver Fed:FedDriver port:" + (BASE_CLIENT_PORT + client);
				// Distributed clients connect to the remote server's IP address.
				a += " -pylon webSocket:pylon" + client + " connectTo:" + WS_SERVER + WS_SERVER_PORT;
				a += " -agent " + Constants.CLIENT_AGENT_PREFIX + client +
						" in-context-of:Fed:FedDriver" +
						" -shard messaging" +
						" -shard FedClient" +
						" server_agent_id:agentS" +
						" dataset:cifar10" +
						" partition_id:" + (client - 1) +  // 0-based index
						" num_partitions:" + nclients +
						" device:cpu";
			}
			// LOCAL CLIENTS
			else if (hostIndex == 0 && client > num_distributed_clients) {
				a += " -node nodeClient" + client + " keep -driver Fed:FedDriver port:" + (BASE_CLIENT_PORT + client);
				// Local clients connect to the server via localhost.
				a += " -pylon webSocket:pylon" + client + " connectTo:" + MY_WS_SERVER + WS_SERVER_PORT;
				a += " -agent " + Constants.CLIENT_AGENT_PREFIX + client +
						" in-context-of:Fed:FedDriver" +
						" -shard messaging" +
						" -shard FedClient" +
						" server_agent_id:agentS" +
						" dataset:cifar10" +
						" partition_id:" + (client - 1) + // 0-based index
						" num_partitions:" + nclients +
						" device:cpu";
			}
		}

		// Only execute if a configuration has been generated for this host.
		if (!a.isEmpty()) {
			FlashBoot.main(a);
		} else {
			System.out.println("No roles for hostIndex " + hostIndex + " with the current configuration.");
		}
	}
}
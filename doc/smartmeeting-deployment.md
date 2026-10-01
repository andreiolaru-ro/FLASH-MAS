# Smart Meeting multi-machine deployment

Scenario: `resources/config/smartmeeting/tree-61n-3nodes-100a.json` : 1 auction agent, 40 person agents, 60 room agents on a 61-node tree graph,
split over 3 FLASH-MAS nodes connected through the websocket pylon.

| Node        | Agents                   | Pylon                                   |
|-------------|--------------------------|-----------------------------------------|
| `sm-node-1` | `auction0`, `person0–39` | websocket server (port 8899)            |
| `sm-node-2` | `r1–r30`                 | websocket client, connects to the server |
| `sm-node-3` | `r31–r60`                | websocket client, connects to the server |

The first node listed in `deployment.nodes` hosts the websocket server; all other nodes
connect to it. Nodes can be placed on machines in any combination, one node per machine,
or several nodes on the same machine (one JVM per node).

## 1. Getting the code on each machine

Option A — build on every machine:

```bash
git clone <repo-url> && cd FLASH-MAS && git checkout abms
mvn -q compile dependency:build-classpath -Dmdep.outputFile=cp.txt -DskipTests
CP="target/classes:$(cat cp.txt)"
```

Option B — build once, run everywhere:

```bash
# on the build machine
mvn -q package -DskipTests        # produces target/Flash-MAS-0.0.1-SNAPSHOT-jar-with-dependencies.jar
# commit/copy the jar + resources/config/, then on every machine:
CP="target/Flash-MAS-0.0.1-SNAPSHOT-jar-with-dependencies.jar"
```

The fat jar contains the `src-experiments` classes and all dependencies, so the other
machines only need a JRE.

On Windows, use `;` instead of `:` as the classpath separator.

## 2. Running

Start the node hosting the websocket server first, then the others. Every other node
receives the server machine's IP through `-Dsmartmeeting.ws.host`.

```bash
# node hosting the websocket server
java -cp "$CP" abms.smartMeeting.boot.SmartMeetingDistributedBoot \
    resources/config/smartmeeting/tree-61n-3nodes-100a.json --node=sm-node-1

# every other node
java -Dsmartmeeting.ws.host=<server-IP> -cp "$CP" abms.smartMeeting.boot.SmartMeetingDistributedBoot \
    resources/config/smartmeeting/tree-61n-3nodes-100a.json --node=sm-node-2

java -Dsmartmeeting.ws.host=<server-IP> -cp "$CP" abms.smartMeeting.boot.SmartMeetingDistributedBoot \
    resources/config/smartmeeting/tree-61n-3nodes-100a.json --node=sm-node-3
```

The port is 8899 by default; override it on **all** nodes with `-Dsmartmeeting.ws.port=<port>`.
The server machine's firewall must allow inbound TCP on that port. To check connectivity
from another machine while the server node runs: `nc -vz <server-IP> 8899`.

### Timing tolerances

- The auction agent resends the RFP to silent rooms every 1 s and waits up to 15 s per
  auction (`bidTimeoutMillis` in the scenario's Auction params), so all nodes should be
  started within ~15 s of each other. Increase `bidTimeoutMillis` (same value on every
  machine) for more slack.
- Each node runs for `steps × stepPeriodMs` = 2400 × 50 ms = **120 s**, then exports its
  trace and shuts down.

## 3. Results

Each JVM writes the events of *its own* agents under `results/smartmeeting/distributed/`
(relative to its working directory). The run metrics (auctions started/won, bids received,
acceptance rate, winner distribution) are computed from events recorded by the auction
and person agents, so the complete `run-000-summary.json` is the one on the node hosting
them (`sm-node-1` in this scenario). The other nodes' traces contain the room-side events
(`bid-created`, `reservation-confirmed`).

## 4. Several nodes on one machine

Run one JVM per node, using `localhost` (or the server IP) as the host, and give each
extra JVM a distinct results directory so they do not overwrite each other's trace files:

```bash
java -cp "$CP" abms.smartMeeting.boot.SmartMeetingDistributedBoot resources/config/smartmeeting/tree-61n-3nodes-100a.json --node=sm-node-1
java -Dsmartmeeting.results.dir=results/sm-node-2 -cp "$CP" abms.smartMeeting.boot.SmartMeetingDistributedBoot resources/config/smartmeeting/tree-61n-3nodes-100a.json --node=sm-node-2
java -Dsmartmeeting.results.dir=results/sm-node-3 -cp "$CP" abms.smartMeeting.boot.SmartMeetingDistributedBoot resources/config/smartmeeting/tree-61n-3nodes-100a.json --node=sm-node-3
```

Without `--node`, all nodes start in a single JVM.

The same scenario file also runs in pure simulation (single JVM, no pylon):

```bash
java -cp "$CP" abms.smartMeeting.boot.SmartMeetingBoot resources/config/smartmeeting/tree-61n-3nodes-100a.json --runs 1
```

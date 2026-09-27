package example.composite;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment example demonstrating a custom shard that collects and sends agent events.
 * 
 * This example shows:
 * 1. A custom EventCollectorShard that monitors agent events
 * 2. Inter-agent communication via messaging shard
 * 3. Periodic sending of collected data (every 5 seconds)
 * 4. Integration of custom shards with standard shards (messaging, PingTest, PingBackTest)
 * 
 * Deployment explanation:
 * - AgentA:
 *   * PingBackTestShard: receives ping messages and transforms them into AGENT_WAVE events
 *   * EventCollectorShard: collects all events and sends them to AgentB every 5 seconds
 *   * messaging shard: enables inter-agent communication
 *   * EchoTesting: monitors events
 * 
 * - AgentB:
 *   * PingTestShard: sends ping messages to AgentA at regular intervals
 *   * messaging shard: enables inter-agent communication
 *   * EchoTesting shard: monitors events
 * 
 * How it works:
 * 1. Both agents start
 * 2. PingTestShard in AgentB starts sending ping messages to AgentA every 2 seconds
 * 3. PingBackTestShard in AgentA receives ping messages and transforms them into AGENT_WAVE events
 * 4. EventCollectorShard in AgentA collects all events (including AGENT_WAVEs from pings)
 * 5. Every 5 seconds, EventCollectorShard sends the collected event list to AgentB
 * 6. This demonstrates a custom shard that monitors and collects all agent activity
 * 
 * Deployment arguments breakdown:
 * - "-package testing example.composite -loader agent:composite"
 *   Load agents and shards from both "testing" (PingTest, PingBackTest, EchoTesting) 
 *   and "example.composite" (EventCollectorShard) packages
 * 
 * - "-node node1"
 *   Single node hosting both agents
 * 
 * - "-agent composite:AgentA -shard messaging -shard PingBackTest -shard EventCollector targetAgent:AgentB -shard EchoTesting"
 *   AgentA with messaging, PingBackTest (receiver), EventCollector (custom), and monitoring
 * 
 * - "-agent composite:AgentB -shard messaging -shard PingTest otherAgent:AgentA keep -shard EchoTesting"
 *   AgentB with messaging, PingTest (sender), and monitoring
 * 
 * @author Mario
 */
public class BootEventCollector {
    /**
     * Main method that constructs and executes the deployment.
     *
     * @param args_ - not used
     */
    public static void main(String[] args_) {
        String args = "";
        
        // Package and loader configuration - load from both testing and example.composite packages
        // testing: contains PingTestShard, PingBackTestShard, EchoTestingShard
        // example.composite: contains EventCollectorShard
        args += " -package testing example.composite -loader agent:composite";
        
        // Node configuration
        args += " -node node1";
        
        // AgentA: receives pings, collects events, and sends them to AgentB
        // - messaging: enables inter-agent communication
        // - PingBackTest: receives ping messages from AgentB and transforms them into AGENT_WAVE events
        // - EventCollector targetAgent:AgentB: custom shard that collects all events (including pings) and sends to AgentB every 5 sec
        // - EchoTesting: monitors all agent events (logging)
        args += " -agent composite:AgentA -shard messaging -shard PingBackTest -shard EventCollector targetAgent:AgentB -shard EchoTesting";
        
        // AgentB: sends pings to AgentA and receives event lists
        // - messaging: enables inter-agent communication
        // - PingTest otherAgent:AgentA: sends ping messages to AgentA (generates AGENT_WAVE events for AgentA to collect)
        // - EchoTesting: monitors all agent events (logging)
        args += " -agent composite:AgentB -shard messaging -shard PingTest otherAgent:AgentA keep -shard EchoTesting";

        FlashBoot.main(args.split(" "));
    }
}


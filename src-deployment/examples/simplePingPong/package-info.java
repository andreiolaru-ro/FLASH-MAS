/**
 * 
 */
/**
 * The scenario of two agents which ping messages between them (one sends and one replies).
 * <p>
 * Run this with:
 * <p>
 * <code>-package examples.compositeAgent -loader agent:composite -agent composite:AgentA -shard messaging -shard PingTestComponent otherAgent:AgentB -shard MonitoringTest -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";</code>
 * <p>
 * Expect to see at each 2 seconds 2 events: one from AgentB and one from AgentA.
 * 
 * @author Andrei Olaru
 */
package examples.simplePingPong;
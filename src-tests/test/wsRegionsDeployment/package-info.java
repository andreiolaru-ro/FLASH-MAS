/**
 * 
 */
/**
 * Tests with the wsRegions communication infrastructure.
 * 
 * Scripts in Script package:
 * <ul>
 * <li>Intense scripts are intense exchanges of messages between static agents.
 * <li>In the Isolated version, there are no messages exchanged between regions.
 * <li>Moving scripts contain one agent moving around the system, replying to messages from the other agents.
 * <li>In "MovingI" versions, the agent starts moving immediately after arriving. In the other versions it waits for
 * sometime before moving again.
 * </ul>
 * For each script, the distributed version contains additional synchronization mechanisms so that exchanges start
 * virtually at the same time.
 * 
 * Running the scripts on multiple machines (on Windows / Linux):
 * <p>
 * <code>
 * git clone -b shadow-integration https://github.com/andreiolaru-ro/FLASH-MAS.git
 * <p>
 * java -cp "bin;lib/core/*;lib/gui/*;lib/*;lib/websocket/*" wsRegionsDeployment.BootMovingDist 0
 * <p>
 * java -cp "bin:lib/core/*:lib/gui/*:lib/*:lib/websocket/*" wsRegionsDeployment.BootMovingDist 0
 * </code>
 * 
 * @author Andrei Olaru
 *
 */
package test.wsRegionsDeployment;
/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package test.runnableCompositePingPong;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.node.Node;

/**
 * Deployment testing.
 */
public class Boot {
	/**
	 * Stop after this time.
	 */
	public static long RUN_TIME = 5000;
	
	/**
	 * Performs test.
	 * 
	 * @param args
	 *            - not used.
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException {
		String test_args = "";
		
		test_args += " -package testing test.runnableCompositePingPong -loader agent:composite";
		test_args += " -node main classpath:NodeAccess";
		test_args += " -pylon local:main-pylon use-thread";
		test_args += " -agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentB -shard EchoTesting";
		test_args += " -agent composite:AgentB -shard messaging -shard PingBackTest -shard EchoTesting";
		
		List<Node> nodes = new Deployment().loadDeployment(Arrays.asList(test_args.split(" ")));
		NodeAccess node = (NodeAccess) nodes.get(1); // the 0-th node is node <null>
		// node.start();
		// Thread.sleep(RUN_TIME);
		// node.stop();
		
		// in comments below an example where sending and delivering the messages is done at a distance
		// LocalSupport pylon = null;
		for(Entity<?> e : node.getOtherEntities())
			// if(e.getName().equals("main-pylon support"))
			// pylon = (LocalSupport) e;
			// else
			e.start();
		Set<Thread> threads = new HashSet<>();
		for(Entity<?> e : node.getAgents())
			threads.add(new Thread((Runnable) e));
		for(Thread t : threads)
			t.start();
		Thread.sleep(RUN_TIME);
		// pylon.start();
		// Thread.sleep(1000);
		for(Entity<?> e : node.getAgents())
			e.stop();
		for(Entity<?> e : node.getOtherEntities())
			e.stop();
		for(Thread t : threads)
			t.join();
		System.out.println("Done.");
	}
	
}

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
package test.compositePingPong;

import andrei.partialCLI.PartialCLI_Test;
import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.node.clientApp.ClientApp;
import net.xqhs.flash.rmi.NodeCLI;
import net.xqhs.flash.testViorel.PartialCLIWrapp;
import net.xqhs.flash.testViorel.Timer;

import java.util.concurrent.TimeUnit;


/**
 * Deployment testing.
 */
public class Boot
{
	/**
	 * Designation for shards.
	 */
	public static final String	FUNCTIONALITY	= "TESTING";
	/**
	 * Different designation for shards.
	 */
	public static final String	MONITORING		= "OTHER-MONITORING";
	
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		String args = "";
		
		args += " -package testing -loader agent:composite";
		args += " -node node1";
		// notice how the name of the shard does not necessarily need to contain "Shard", as it is handled by autoFind
		args += " -agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentB -shard EchoTesting";
		args += " -agent composite:AgentB -shard messaging -shard PingBackTest -shard EchoTesting";
		
		FlashBoot.main(args.split(" "));


		PartialCLIWrapp.processArgs(args.split(" "));


	}


	
}

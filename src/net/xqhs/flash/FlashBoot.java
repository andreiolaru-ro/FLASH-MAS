/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash;
import java.util.Arrays;

import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.logging.Logging;

/**
 * Class that boots a Flash-MAS instance.
 * 
 * @author andreiolaru
 */
public class FlashBoot
{
	/**
	 * Main method. It calls {@link NodeLoader#load(TreeParameterSet)} with the arguments received by the program.
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 */
	public static void main(String[] args)
	{
		Logging.getMasterLogging().setLogLevel(Level.ALL);
		String test_args;
		// test_args = "";
		// test_args = "src-deployment/ChatAgents/deployment-chatAgents.xml";
		test_args = "src-deployment/simpleDeployment/simpleDeployment.xml";
		// test_args = "-support local:auxilliary host:here -agent bane something:something -component a";
		// test_args = "-agent AgentA some:property -feature mobility where:anywhere -support local:auxilliary host:here
		// -agent bane something:something -othercomponent a";
		// test_args = "-support local -support local arg:val -support last host:here -agent bane something:something
		// -feature a -feature b par:val -feature c -agent bruce -feature a";
		String[] use_args = test_args.split(" ");
		
		
		new NodeLoader().load(new TreeParameterSet().addAll("args", Arrays.asList(use_args)));
//		new Boot().boot(.split(" "));
	}
	
}

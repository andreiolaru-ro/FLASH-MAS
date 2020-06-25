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

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.logging.Logging;
import net.xqhs.util.logging.wrappers.GlobalLogWrapper;

/**
 * Class that boots a Flash-MAS instance.
 * 
 * @author andreiolaru
 */
public class FlashBoot
{
	/**
	 * Main method. It calls {@link NodeLoader#loadDeployment} with the arguments received by the program.
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 */
	public static ByteArrayOutputStream stream = null;
	public static void main(String[] args)
	{
		Logging.getMasterLogging().setLogLevel(Level.ALL);

		stream = new ByteArrayOutputStream();
		GlobalLogWrapper.setLogStream(stream);
		List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(args));
		for(Node node : nodes)
			node.start();
	}
	
}

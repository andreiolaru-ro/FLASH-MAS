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
package net.xqhs.flash;

import java.util.Arrays;
import java.util.List;

import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.MasterLog;

/**
 * Class that boots a Flash-MAS instance.
 * 
 * @author andreiolaru
 */
public class FlashBoot
{
	// public static ByteArrayOutputStream stream = null;
	/**
	 * Main method. It calls {@link NodeLoader#loadDeployment} with the arguments received by the program.
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 */
	public static void main(String[] args)
	{
		MasterLog.setLogLevel(Level.ALL);

		// stream = new ByteArrayOutputStream();
		// GlobalLogWrapper.setLogStream(stream);
		List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(args));
		// try {
		// Thread.sleep(20000);
		// } catch(InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		for(Node node : nodes)
			node.start();
	}
	
}

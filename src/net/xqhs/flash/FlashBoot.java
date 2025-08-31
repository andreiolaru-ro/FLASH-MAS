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

import java.io.FileNotFoundException;
import java.io.PipedOutputStream;
import java.util.Arrays;
import java.util.List;

import easyLog.EasyLog;
import easyLog.integration.BufferedStreamOutput;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.MasterLog;
import net.xqhs.util.logging.output.ConsoleOutput;
import net.xqhs.util.logging.output.FileOutput;

/**
 * Class that boots a Flash-MAS instance.
 * 
 * @author andreiolaru
 */
public class FlashBoot {
	
	protected static int		bitIndex			= 0;
	protected static final int	OUTPUT_TO_CONSOLE	= 1 << bitIndex++;
	protected static final int	OUTPUT_TO_FILE		= 1 << bitIndex++;
	protected static final int	OUTPUT_TO_EASYLOG	= 1 << bitIndex++;
	// protected static final int LOG_OUTPUTS = OUTPUT_TO_CONSOLE;
	protected static final int LOG_OUTPUTS = OUTPUT_TO_EASYLOG | OUTPUT_TO_CONSOLE;
	// protected static final int LOG_OUTPUTS = OUTPUT_TO_FILE | OUTPUT_TO_EASYLOG | OUTPUT_TO_CONSOLE;
	protected static final Level	GLOBAL_LOG_LEVEL	= Level.ALL;
	private static final String		EASYLOG_CONFIG_FILE	= "resource/eumas.yml";
	
	/**
	 * Main method. It calls {@link NodeLoader#loadDeployment} with the arguments received by the program.
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 */
	public static void main(String[] args) {
		MasterLog.setLogLevel(GLOBAL_LOG_LEVEL);
		if((LOG_OUTPUTS & OUTPUT_TO_CONSOLE) != 0)
			MasterLog.addDefaultOutput(new ConsoleOutput());
		
		if((LOG_OUTPUTS & OUTPUT_TO_FILE) != 0)
			try {
				MasterLog.addDefaultOutput(new FileOutput("global.log"));
			} catch(FileNotFoundException e) {
				e.printStackTrace();
			}
		
		PipedOutputStream stream = new PipedOutputStream();
		if((LOG_OUTPUTS & OUTPUT_TO_EASYLOG) != 0) {
			MasterLog.addDefaultOutput(new BufferedStreamOutput(stream));
			EasyLog.start(stream, EASYLOG_CONFIG_FILE);
		}
		
		List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(args));
		for(Node node : nodes)
			node.start();
	}
}

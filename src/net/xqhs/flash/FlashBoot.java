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

import java.io.*;
import java.util.Arrays;
import java.util.List;

import easyLog.src.main.java.main.Main;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.MasterLog;
import net.xqhs.util.logging.output.ConsoleOutput;
import net.xqhs.util.logging.output.FileOutput;
import net.xqhs.util.logging.output.StreamLogOutput;

/**
 * Class that boots a Flash-MAS instance.
 * 
 * @author andreiolaru
 */
public class FlashBoot {
	/**
	 * Main method. It calls {@link NodeLoader#loadDeployment} with the arguments received by the program.
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 */
	public static void main(String[] args) {
		MasterLog.setLogLevel(Level.ALL);
//		MasterLog.addDefaultOutput(new ConsoleOutput());
		try {
			MasterLog.addDefaultOutput(new FileOutput("output/global.log"));

			StreamLogOutput out = new StreamLogOutput() {
				//easyLog Wrapper
				PipedOutputStream out = new PipedOutputStream();

				@Override
				public long getUpdatePeriod() {
					return 0;
				}

				@Override
				public int formatData() {
					return Logger.INCLUDE_NAME;
				}

				@Override
				public boolean useCustomFormat() {
					return false;
				}

				@Override
				public String format(Level level, String s, String s1) {
					return null;
				}

				@Override
				public void update() {
					// daca e nevoie sa fac flush Stream.flush
					try {
						out.flush();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

				}

				@Override
				public OutputStream getOutputStream() {
					return out;
				}

				@Override
				public void exit() {
					// inchid streamul
					try {
						out.close();
					} catch (IOException e) {
						throw new RuntimeException(e);
					}

				}
			};
			MasterLog.addDefaultOutput(out);

			PipedInputStream in = new PipedInputStream();
			PipedOutputStream easyLogOut = (PipedOutputStream) out.getOutputStream();
			easyLogOut.connect(in);

			Thread easyLog = new Thread(() -> {
				try {
					Main.main(in);
				} catch (IOException e) {
					throw new RuntimeException(e);
				} catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
			easyLog.start();
		} catch(FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<Node> nodes = new NodeLoader().loadDeployment(Arrays.asList(args));
		for(Node node : nodes)
			node.start();
	}
	
}

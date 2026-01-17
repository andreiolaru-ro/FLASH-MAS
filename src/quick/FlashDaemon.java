/*******************************************************************************
 * Copyright (C) 2021 Andrei Olaru.
 * * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package quick;

import net.xqhs.flash.daemon.FlashMasDaemon;

/**
 * Entry point for the Flash-MAS Node Daemon.
 * <p>
 * Parses command line arguments and starts the daemon instance.
 * usage: java quick.FlashDaemon [port] [-console]
 */
public class FlashDaemon {

	public static void main(String[] args) {
		int port = 35274;
		boolean redirectOutput = false;

		// Parse arguments
		for (String arg : args) {
			if ("-redirect".equals(arg)) {
				redirectOutput = true;
			} else {
				try {
					port = Integer.parseInt(arg);
				} catch (NumberFormatException e) {
					System.err.println("Invalid port argument: " + arg + ". Using default: " + port);
				}
			}
		}

		System.out.println("Starting Flash-MAS Daemon...");
		System.out.println("Mode: " + (redirectOutput ? "FILE LOGGING" : "CONSOLE OUTPUT"));

		FlashMasDaemon daemon = new FlashMasDaemon(port, redirectOutput);
		daemon.start();
	}
}
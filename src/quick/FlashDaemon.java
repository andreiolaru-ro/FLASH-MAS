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
 * Usage: java quick.FlashDaemon [-port <number>] [-redirect]
 */
public class FlashDaemon {

	public static void main(String[] args) {
		new FlashMasDaemon().start(args);
	}

}
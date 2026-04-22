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
package test.guiGeneration;

import net.xqhs.flash.FlashBoot;

/**
 * All-In-One Deployment testing for the Web Visualizations Framework.
 * This runs agents that can populate all 4 types of charts (Scatter, Line, Bar, Graph).
 */
public class BootComprehensiveTest {
	/**
	 * Performs test.
	 *
	 * @param args
	 *                 - not used.
	 */
	public static void main(String[] args) {
		String test_args = "";

		test_args += " -loader agent:composite";
		test_args += " -package test.guiGeneration";

		test_args += " -node main central:web";

		test_args += " -pylon webSocket:serverPylon serverPort:8886";

		for (int i = 1; i <= 50; i++) {
			test_args += " -agent composite:Agent" + i 
			             + " -shard messaging"
			             + " -shard remoteOperation"
			             + " -shard ComprehensiveTest";
		}

		FlashBoot.main(test_args.split(" "));
	}
}

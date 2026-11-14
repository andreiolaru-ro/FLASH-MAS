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
 *
 * <p>The {@code update-frequency} parameter on the {@code remoteOperation} shard
 * controls how often agent data is forwarded to the UI (in milliseconds).
 * Change it to tune the update rate:
 * <ul>
 *   <li>{@code update-frequency:1000}  – every 1 second (original behaviour)
 *   <li>{@code update-frequency:5000}  – every 5 seconds
 *   <li>{@code update-frequency:-1}    – no rate limiting, send immediately
 * </ul>
 *
 * <p>{@link ComprehensiveTestShard} no longer owns its own timer; it registers
 * itself as a {@link net.xqhs.flash.remoteOperation.PropertyContainer}
 * so that {@code RemoteOperationShard} drives all sends at the configured rate.
 */
public class BootComprehensiveTest {

	/**
	 * Update frequency sent to the M&C entity, in milliseconds.
	 * Change this single constant to adjust the UI refresh rate.
	 */
	private static final int UPDATE_FREQUENCY_MS = 5000;

	public static void main(String[] args) {
		String test_args = "";

		test_args += " -loader agent:composite";
		test_args += " -package test.guiGeneration";

		test_args += " -node main central:web";

		test_args += " -pylon webSocket:serverPylon serverPort:8886";

		for (int i = 1; i <= 50; i++) {
			test_args += " -agent composite:Agent" + i
					+ " -shard messaging"
					// update-frequency controls the minimum interval between UI updates (ms).
					// -1 = no rate limiting (send immediately on every change).
					+ " -shard remoteOperation update-frequency:" + UPDATE_FREQUENCY_MS
					+ " -shard ComprehensiveTest";
		}

		FlashBoot.main(test_args.split(" "));
	}
}
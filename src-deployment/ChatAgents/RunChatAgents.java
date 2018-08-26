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
package ChatAgents;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.core.node.BootDefaultSettings;

/**
 * Simple class running the scenario present in the same folder with it.
 * 
 * @author Andrei Olaru
 */
public class RunChatAgents
{
	/**
	 * Runs the file scenario.xml in the same directory as this class.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args)
	{
		String cp = RunChatAgents.class.getName();
		String scenarioPath = BootDefaultSettings.SCENARIO_DIRECTORY
				+ cp.substring(0, cp.lastIndexOf(".")).replace(".", "/") + "/deployment-ChatAgents.xml";
		FlashBoot.main(new String[] { scenarioPath });
	}
}

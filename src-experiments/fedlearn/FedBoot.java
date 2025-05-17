/*******************************************************************************
 * Copyright (C) 2023 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package fedlearn;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.fedlearn.Constants;

/**
 * Runs scenario.
 */
public class FedBoot {
	
	/**
	 * Performs test.
	 *
	 * @param args
	 *            - not used.
	 */
	public static void main(final String[] args) {
		String a = "";
		
		a += " -load_order driver;pylon;agent -loader agent:composite";
		a += " -package testing net.xqhs.flash.fedlearn " + FedBoot.class.getPackageName();
		
		int nclients = 5;
		
		a += " -node nodeServer -driver Fed:FedDriver";
		a += " -pylon local:pylon1";
		a += " -agent agentS  in-context-of:Fed:FedDriver -shard echoTesting exit:5 -shard messaging -shard FedServer nclients:"
				+ nclients;
		for(int client = 0; client++ < nclients;)
			a += " -agent " + Constants.CLIENT_AGENT_PREFIX + client
					+ " in-context-of:Fed:FedDriver -shard FedClient -shard messaging -shard echoTesting exit:5";
		
		FlashBoot.main(a);
	}
}

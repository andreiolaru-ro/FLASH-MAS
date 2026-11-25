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
package jason;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class JasonBoot {
	enum Test {
		ONE, TWO, COMMS
	}
	
	// static Test test = Test.ONE;
	static Test test = Test.COMMS;
	
	/**
	 * Performs test.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		String test_args = "";
		
		switch(test) {
		case ONE:
			test_args += " -agent jason:printer load:simpleSlow.asl";
			break;
		case TWO:
			test_args += " -agent jason:slow load:simpleSlow.asl";
			test_args += " -agent jason:quick load:simpleQuick.asl";
			break;
		case COMMS:
			test_args += " -agent jason:alice load:ping.asl";
			test_args += " -agent jason:bob load:pong.asl";
			break;
		default:
			break;
		}
		
		FlashBoot.main(test_args.split(" "));
	}
	
}

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
package example.helloWorld;

import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.agent.BaseAgent;

/**
 * Simple agent for testing. Logging is handled by {@link EntityCore}.
 *
 * @author Andrei Olaru
 */
public class HelloWorldAgent extends BaseAgent {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		li("Hello World");
		stop();
		return true;
	}
}

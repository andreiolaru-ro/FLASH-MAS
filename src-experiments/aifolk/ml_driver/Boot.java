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
package aifolk.ml_driver;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class Boot
{
	/**
	 * Designation for shards.
	 */
	public static final String FUNCTIONALITY = "TESTING";
	
	/**
	 * Performs test
	 * 
	 * @param args_
	 *            - not used.
	 */
	public static void main(String[] args_)
	{
		String args = "";
		
		args += " -package aifolk.ml_driver -package net.xqhs.flash.ml -loader agent:composite";
		args += " -pylon MLDriver:mldriver";
		args += " -pylon local:default";
		args += " -agent composite:AgentA in-context-of:MLDriver:mldriver -shard messaging -shard EchoTesting -shard MLTesting";
		
		FlashBoot.main(args.split(" "));
	}
	
}

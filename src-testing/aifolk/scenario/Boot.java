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
package aifolk.scenario;

import net.xqhs.flash.FlashBoot;

/**
 * Runs scenario.
 */
public class Boot {

    /**
     * Performs test.
     *
     * @param args
     *                 - not used.
     */
    public static void main(String[] args)
    {
		String a = "";

		a += " -pylon local:";
		a += " -scenario Exp";
		a += " -agent A -shard ExpDriver -shard messaging";
		a += " -agent B -shard ExpDriver -shard messaging";
		a += " -agent C -shard ExpDriver -shard messaging";

		FlashBoot.main(a.split(" "));
    }
}

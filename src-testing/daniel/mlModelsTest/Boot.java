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
package daniel.mlModelsTest;

import net.xqhs.flash.FlashBoot;

public class Boot {

    /**
     * Performs test.
     *
     * @param args
     *                 - not used.
     */
    public static void main(String[] args)
    {
        String test_args = "";

        test_args += " -package example.compositePingPong";
        test_args += " -package daniel.mlModelsTest";
        test_args += " -package net.xqhs.flash.mlModels";
        test_args += " -loader agent:composite";

        // Exemplul 1 - NU MERGE

        // test_args += " -node node1";
		// test_args += " -pylon local:ml-runner classpath:MLRunnerPylon";

        // test_args += " -agent composite:AgentA -shard messaging -shard MLDescriptionShard -shard PingBackTestComponent";
        // test_args += " -agent composite:AgentB -shard messaging -shard MLDescriptionShard -shard PingTestComponent otherAgent:AgentA";
        


        // Exemplul 2 - NU MERGE
    

        // test_args += " -node node1";
        // test_args += " -pylon local:default";
        // test_args += " -pylon local:ml-runner classpath:MLRunnerPylon";

        // test_args += " -agent composite:AgentA -shard messaging -shard MLDescriptionShard -shard PingBackTestComponent";
        // test_args += " -agent composite:AgentB -shard messaging -shard MLDescriptionShard -shard PingTestComponent otherAgent:AgentA";



        // Exemplul 3 - MERGE


        test_args += " -node node1";
		test_args += " -pylon local:ml-runner classpath:MLRunnerPylon";
        test_args += " -pylon local:main";

        test_args += " -agent composite:AgentA -shard messaging -shard MLDescriptionShard -shard PingBackTestComponent";
        test_args += " -agent composite:AgentB -shard messaging -shard MLDescriptionShard -shard PingTestComponent otherAgent:AgentA";



        // Exemplul 4 - MERGE


		// test_args += " -node node1";
        // test_args += " -pylon local:ml-runner classpath:MLRunnerPylon";
        // test_args += " -pylon webSocket:slave1 serverPort:8886 connectTo:ws://localhost:8886";
		// test_args += " -agent composite:AgentA -shard messaging -shard MLDescriptionShard -shard PingBackTestComponent";

		// test_args += " -node node2";
        // test_args += " -pylon webSocket:slave2 connectTo:ws://localhost:8886";
		// test_args += " -agent composite:AgentB -shard messaging -shard MLDescriptionShard -shard PingTestComponent otherAgent:AgentA";


        FlashBoot.main(test_args.split(" "));
    }
}

<!--- ---------------------------------------------
Copyright (C) 2021 Andrei Olaru.

This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.

Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
--------------------------------------------- -->

Since the libraries have been organized on directories, there is a script that gathers all jars in a single directory. The script should be run **from the project root directory** and can be found at `script/make-lib-all.sh` .

Running FLASH-MAS should be as simple as:

* in Linux:
  
  ```bash
  java -cp "bin:lib-all/*" quick.Boot <args>
  ```

* in Windows:
  
  ```bash
  java -cp "bin;lib-all/*" quick.Boot <args>
  ```

To run the project from an IDE, you can run the class `test.compositePingPong.Boot` (in source directory `src-tests`), which contains a `main` method. The expected results are in the `package-info.java` file for that package. Good tests for websocket-based communication are the classes with names starting with `Boot` in the package `test.webSocketDeployment`.

See also [importing.md](importing.md).

To run an example from the terminal, you can run an equivalent of the scenario above with (from the root directory of the project)
```bash
java -cp "bin;lib-all/*" quick.Boot -package testing -loader agent:composite -node node1 -agent composite:AgentA -shard messaging -shard PingTest otherAgent:AgentB -shard EchoTesting -agent composite:AgentB -shard messaging -shard PingBackTest -shard EchoTesting
```

One "simple deployment" WebSocket test can be run with
```bash
java -cp "bin;lib-all/*" quick.Boot -package test.simplePingPong -node node1 -pylon webSocket:pylon1 serverPort:8885 -agent AgentA classpath:AgentPingPong sendTo:AgentB -node node2 -pylon webSocket:pylon2 connectTo:ws://localhost:8885 -agent AgentB classpath:AgentPingPong
```

<!--- ---------------------------------------------
Copyright (C) 2021 Andrei Olaru.

This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.

Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.

Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
--------------------------------------------- --> 

### Concept Names translation from tATAmI to FLASH-MAS

* (tATAmI-2 name -> FLASH-MAS name
* simulation (in terms of classes in the code) -> deployment
* simulation (in terms of the process of running an experiment) -> simulation
* visualization -> net.xqhs.flash.core.monitoring / control
* component -> feature -> shard
* platform -> support infrastructure + pylon
   * the support infrastructure is the system-spanning virtual entity that offers services such as mobility
   * the pylon is this entity's concrete presence on a node
* ParameterSet -> MultiValueMap
* TreeParameterSet -> MultiTreeMap
* Support pylons (or pylons, for short)

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
package net.xqhs.flash.mlModels;

import java.io.IOException;
import java.util.Set;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.config.Config;
import net.xqhs.util.config.Config.ConfigLockedException;
import net.xqhs.util.config.Configurable;

/**
 * 
 *
 *  @author Daniel Liurca
 */
public class MLRunnerPylon implements Pylon {
	private Process process;

	/**
	 * 
	 *
	 * @return an indication of success.
	 */
	@Override
	public boolean start() {
		System.out.println("ML PYLON STARTED");

		try {
			process = Runtime.getRuntime().exec("python PythonModule/server.py");
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}

		return true;
	}
	
	@Override
	public boolean stop() {
		process.destroy();
		return true;
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {

		return true;
	}

	@Override
	public boolean addContext(EntityProxy<Node> context) {
		return true;
	}

	@Override
	public boolean isRunning() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <C extends Entity<Node>> EntityProxy<C> asContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Configurable makeDefaults() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Config lock() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Config build() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void ensureLocked() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void locked() throws ConfigLockedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<String> getSupportedServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getRecommendedShardImplementation(AgentShardDesignation shardType) {
		// TODO Auto-generated method stub
		return null;
	}
}

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
package testing;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.UnitComponent;

/**
 * An {@link AgentShard} implementation that monitors all agent events.
 * <p>
 * It also allows to set a time after which the agent should exit, via the {@value #EXIT_PARAMETER_NAME} argument for
 * this shard (in seconds).
 * 
 * @author Andrei Olaru
 */
public class EchoTestingShard extends AgentShardCore {
	/**
	 * The UID.
	 */
	private static final long		serialVersionUID	= 5214882018809437402L;
	/**
	 * Shard designation.
	 */
	public static final String		DESIGNATION			= "test/monitoring";
	/**
	 * The name of the parameter that indicates after how many seconds the agent should exit..
	 */
	protected static final String	EXIT_PARAMETER_NAME	= "exit";
	
	/**
	 * The log.
	 */
	transient UnitComponent	locallog	= null;
	/**
	 * Number of seconds after which the agent should exit. Negative numbers or a zero value are ignored.
	 */
	protected int			exitAfter	= -1;
	/**
	 * The timer for closing the agent.
	 */
	protected Timer			exitTimer	= null;
	
	/**
	 * No-argument constructor
	 */
	public EchoTestingShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.containsKey(EXIT_PARAMETER_NAME))
			exitAfter = Integer.parseInt(configuration.getFirstValue(EXIT_PARAMETER_NAME));
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		String eventMessage = "agent [" + getAgent().getEntityName() + "] event: [" + event.toString() + "]";
		locallog.li(eventMessage);
		// if (getAgentLog() != null)
		// getAgentLog().info(eventMessage);
		if(event.getType().equals(AgentEventType.AGENT_START) && exitAfter > 0) {
			exitTimer = new Timer();
			exitTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					askExit();
				}
			}, exitAfter * 1000);
		}
		if(event.getType() == AgentEventType.AGENT_STOP) {
			if(exitTimer != null)
				exitTimer.cancel();
			locallog.doExit();
		}
	}
	
	/**
	 * Ask the parent agent to exit.
	 */
	protected void askExit() {
		locallog.li("Asking agent to stop since [] seconds have elapsed since start.", Integer.valueOf(exitAfter));
		getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
	}
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		
		if(getAgent() != null) {
			locallog = new UnitComponent("testing-" + getAgent().getEntityName() + " >>>>").setLogLevel(Level.ALL)
					.setLoggerType(PlatformUtils.platformLogType());
			locallog.lf("testing started.");
		}
		else if(locallog != null) {
			locallog.lf("testing stopped.");
			locallog.doExit();
			locallog = null;
		}
	}
}

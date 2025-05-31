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
package test.guiGeneration;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.gui.GuiShard;

/**
 * Periodically increases the value obtained from the port and outputs it back.
 * <p>
 * If the button is activated, the value is updated with whatever the activation sends.
 */
public class TestShard extends AgentShardGeneral {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The timer for auto counting. If autocount is off, the timer will be <code>null</code>.
	 */
	protected Timer					timer	= new Timer();
	/**
	 * The port.
	 */
	protected final static String	PORT	= "valuePort";
	
	/**
	 * No-argument constructor.
	 */
	public TestShard() {
		super(AgentShardDesignation.autoDesignation("Test"));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if("off".equals(configuration.getAValue("autocount")))
			timer = null;
		return super.configure(configuration);
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			((GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation()))
					.sendOutput(new AgentWave(Integer.valueOf(0).toString(), PORT));
			if(timer != null)
				timer.schedule(new TimerTask() {
					@SuppressWarnings("synthetic-access")
					@Override
					public void run() {
						int value = Integer
								.parseInt(((GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation()))
										.getInput(PORT).get(AgentWave.CONTENT));
						((GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation()))
								.sendOutput(new AgentWave(Integer.valueOf(value + 1).toString(), PORT));
					}
				}, 0, 2000);
			
			break;
		case AGENT_STOP:
			li("Stopping agent: ", event);
			if(timer != null) {
				timer.cancel();
				timer = new Timer();
			}
			break;
		case AGENT_WAVE:
			try {
				li("Agent event from []: ", ((AgentWave) event).getCompleteSource(), event);
				((GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation())).sendOutput(new AgentWave(
						Integer.valueOf(Integer.parseInt(event.get(AgentWave.CONTENT))).toString(), PORT));
				break;
			} catch(NumberFormatException e) {
				le("Invalid number format: ", event.get(AgentWave.CONTENT));
			}
			break;
		default:
			break;
		}
	}
}

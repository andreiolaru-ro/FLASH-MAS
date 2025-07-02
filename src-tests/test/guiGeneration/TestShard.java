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
	 * The timer value port.
	 */
	protected final static String	PORT	= "valuePort";
	/** The port for the counter label. */
	protected final static String	COUNTER_PORT = "number";
	/** Increase role for the counter */
	protected final static String	INCREASE_ROLE = "increase";
	/** Decrease role for the counter */
	protected final static String	DECREASE_ROLE = "decrease";
	/** Counter overflow label role */
	protected final static String	INDICATOR_ROLE = "indicator";
	/** Counter value */
	protected static int counter = 0;
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
			handleWave((AgentWave) event);
			break;
		default:
			break;
		}
	}

	public void handleWave(AgentWave wave) {
		li("Agent event from []: []", wave.getCompleteSource(), wave);
		String[] subject = wave.getDestinationElements();
		if (subject.length < 2) return;  // 
		String port = subject[0], role = subject[1];
		GuiShard guiShard = (GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation());
		System.out.println("GUI SHARD: " + guiShard);

		if (COUNTER_PORT.equals(port)) {
			if (INCREASE_ROLE.equals(role)) {
				counter++;
			} else if (DECREASE_ROLE.equals(role)) {
				counter--;
			}

			String indicator = (counter < 0) ? "underflow" : (counter > 10) ? "overflow" : "";
			counter = Math.max(0, Math.min(counter, 10)); // keep counter in [0, 10]
			AgentWave outputWave = new AgentWave(Integer.valueOf(counter).toString(), COUNTER_PORT);
			outputWave.add(INDICATOR_ROLE, indicator);
			guiShard.sendOutput(outputWave);
		}
	}
}

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
package net.xqhs.flash.core.control;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.monitoring.MonitoringShard;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.OperationUtils.ControlOperation;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.gui.GUILoad;
import net.xqhs.flash.gui.structure.Element;

/**
 * Shard to allow controlling the agent from the outside.
 * <p>
 * It adds to the agent UI the Stop button.
 * 
 * @author andreiolaru
 * @author Florina Nastasoiu
 */
public class ControlShard extends AgentShardGeneral {
	/**
	 * The UID.
	 */
	private static final long		serialVersionUID	= 5214882018809437402L;
	/**
	 * Endpoint element for this shard.
	 */
	public static final String		SHARD_ENDPOINT		= StandardAgentShard.CONTROL.shardName();
	/**
	 * File with the UI elements needed by this shard.
	 */
	protected static final String	UI_STRUCTURE		= "controlBtn.yml";
	
	/**
	 * The interface structure required by this shard.
	 */
	protected Element			standartBtn;
	/**
	 * Link to the {@link MonitoringShard}
	 */
	protected MonitoringShard	monitor	= null;
	
	{
		setUnitName("control-shard");
		setLoggerType(PlatformUtils.platformLogType());
	}
	
	/**
	 * No-argument constructor.
	 */
	public ControlShard() {
		super(StandardAgentShard.CONTROL.toAgentShardDesignation());
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		standartBtn = GUILoad.load(new MultiTreeMap().addOneValue(GUILoad.FILE_SOURCE_PARAMETER, UI_STRUCTURE)
				.addOneValue(CategoryName.PACKAGE.s(), this.getClass().getPackage().getName()), getLogger());
		if(standartBtn == null) {
			le("Interface load failed");
			return false;
		}
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_WAVE:
			if(!SHARD_ENDPOINT.equals(((AgentWave) event).getFirstDestinationElement()))
				break;
			AgentWave wave = ((AgentWave) event).removeFirstDestinationElement();
			String operation = wave.getFirstDestinationElement();
			if(operation == null)
				li("Unknown control operation [].", operation);
			else
				switch(ControlOperation.fromOperation(operation)) {
				case START_SIMULATION:
					getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.SIMULATION_START));
					break;
				case STOP:
					getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
					lf("Agent [] was stopped by [].", getAgent().getEntityName(), wave.getCompleteSource());
					break;
				default:
					li("Unhandled control operation [].", operation);
					break;
				}
			break;
		case AGENT_START:
			if(getAgentShard(StandardAgentShard.MONITORING.toAgentShardDesignation()) != null) {
				monitor = (MonitoringShard) getAgentShard(StandardAgentShard.MONITORING.toAgentShardDesignation());
				monitor.addGuiElement(standartBtn);
			}
			break;
		default:
			// nothing to do.
			break;
		}
	}
	
	@Override
	protected MultiTreeMap getShardData() {
		return super.getShardData();
	}
	
}

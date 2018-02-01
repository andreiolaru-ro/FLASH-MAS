/*******************************************************************************
 * Copyright (C) 2018 Andrei Olaru.
 * 
 * This file is part of Flash-MAS. The CONTRIBUTORS.md file lists people who have been previously involved with this project.
 * 
 * Flash-MAS is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or any later version.
 * 
 * Flash-MAS is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with Flash-MAS.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package net.xqhs.flash.core.deployment;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import net.xqhs.flash.core.agent.AgentComponent.AgentComponentName;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.io.AgentActiveIO.InputListener;
import net.xqhs.flash.core.agent.messaging.MessagingComponent;
import net.xqhs.flash.core.agent.visualization.AgentGui;
import net.xqhs.flash.core.agent.visualization.AgentGuiConfig;
import net.xqhs.flash.core.agent.visualization.AgentGui.AgentGuiBackgroundTask;
import net.xqhs.flash.core.agent.visualization.AgentGui.ResultNotificationListener;
import net.xqhs.util.XML.XMLTree.XMLNode;
import net.xqhs.util.logging.UnitComponentExt;
import net.xqhs.windowLayout.WindowLayout;
import tatami.core.util.platformUtils.PlatformUtils;
import tatami.simulation.PlatformLoader.PlatformLink;

/**
 * Singleton class managing the simulation, visualization and agent control on a machine or on a set of machines
 * (possibly all).
 * <p>
 * After the initializations in {@link Boot}, it handles the actual starting and management of agents, as well as
 * creating the events in the scenario timeline.
 * <p>
 * It receives updates from all agents regarding logging messages and parent and location changes.
 * <p>
 * The link with agents uses the agent's VisualizationComponent (or equivalent for non-CompositeAgent instances) and
 * corresponding Vocabulary.
 * <p>
 * It normally offers a GUI or some other kind of UI for the said operations, that can exist outside of the actual agent
 * platform(s).
 * <p>
 * Although not an agent of any platform, the {@link SimulationManager} can be viewed as an agent; for convenience, it
 * implements {@link AgentManager} and it features a GUI based on {@link AgentGui}.
 * <p>
 * This implementation presumes that {@link java.util.Timer} and related classes {@link TimerTask} are available on the
 * execution platform.
 * 
 * @author Andrei Olaru
 */
public class SimulationManager implements AgentManager
{
	/**
	 * Components of the simulation manager GUI.
	 * 
	 * @author Andrei Olaru
	 */
	public enum SimulationComponent {
		/**
		 * Button to create the agents.
		 */
		CREATE,
		
		/**
		 * Button to start simulation.
		 */
		START,
		
		/**
		 * Field showing the simulation time.
		 */
		TIME,
		
		/**
		 * Button to pause simulation.
		 */
		PAUSE,
		
		/**
		 * Button to destroy all agents in the simulation.
		 */
		CLEAR,
		
		/**
		 * Button to exit all platforms completely and close the application.
		 */
		EXIT,
	}
	
	/**
	 * Window type for the simulation manager window.
	 */
	public static final String			WINDOW_TYPE						= "system";
	/**
	 * Window name for the simulation manager window.
	 */
	public static final String			WINDOW_NAME						= "simulation";
	/**
	 * The name of the attribute indicating the time of the event.
	 */
	protected static final String		EVENT_TIME_ATTRIBUTE			= "time";
	/**
	 * The prefix to the name of a simulation agent. The rest of the name will be the name of the platform onto which it
	 * resides.
	 */
	protected static final String		SIMULATION_AGENT_NAME_PREFIX	= "SimAgent-";
	
	/**
	 * Name of the node in the scenario file that contains the event timeline to simulate.
	 */
	protected static final String		TIMELINE_NODE					= "timeline";
	/**
	 * Delay before calling a System exit in case of a failed start.
	 */
	protected static final int			SYSTEM_ABORT_DELAY				= 1000;
	/**
	 * If set, a {@link System#exit(int)} will be called after everything has been theoretically closed (in the case of
	 * normal termination).
	 * <p>
	 * Ideally, this should be set to <code>false</code> and all the threads should exit normally.
	 */
	protected static final boolean		FORCE_SYSTEM_EXIT				= true;
	/**
	 * The log.
	 */
	UnitComponentExt					log								= null;
	/**
	 * The GUI.
	 */
	AgentGui							gui								= null;
	
	/**
	 * Name and {@link PlatformLoader} for all platforms to be started.
	 */
	Map<String, PlatformLoader>			platforms;
	/**
	 * Name and locality indication (container is created locally or remotely) for all containers.
	 */
	protected Map<String, Boolean>		containers						= null;
	/**
	 * {@link AgentCreationData} instances for all agents to be started.
	 */
	Set<AgentCreationData>				agents;
	/**
	 * A map that holds for each platform (identified by name) a simulation agent.
	 */
	Map<String, SimulationLinkAgent>	simulationAgents				= new HashMap<String, SimulationLinkAgent>();
	/**
	 * The list of events in the simulation, as specified by the scenario file.
	 */
	List<XMLNode>						events							= new LinkedList<XMLNode>();
	/**
	 * Current time, in 1/10 seconds.
	 */
	long								time							= 0;
	/**
	 * Indicates whether the simulation is currently paused.
	 */
	boolean								isPaused						= false;
	/**
	 * Indicates whether agents have been created.
	 */
	boolean								agentsCreated					= false;
	/**
	 * The {@link Timer} for simulation time and also the display in the GUI.
	 */
	Timer								theTime							= null;
	
	/**
	 * Creates a new instance, also starting the GUI, based on the map of platforms and their names, the map of agents
	 * and their names (agents are managed by {@link AgentManager} wrappers and the timeline.
	 * 
	 * @param allPlatforms
	 *            - the {@link Map} of platform names and {@link PlatformLoader} instances that are currently started.
	 * @param allContainers
	 *            - the map of container names and information whether the container is created locally or remotely.
	 * @param allAgents
	 *            - a {@link Set} of {@link AgentCreationData} instances, describing all agents to be loaded.
	 * @param timeline
	 *            - the timeline of events, as {@link XMLNode} parsed from the scenario file.
	 */
	public SimulationManager(Map<String, PlatformLoader> allPlatforms, Map<String, Boolean> allContainers,
			Set<AgentCreationData> allAgents, XMLNode timeline)
	{
		log = (UnitComponentExt) new UnitComponentExt().setUnitName("simulation").setLoggerType(
				PlatformUtils.platformLogType());
		platforms = allPlatforms;
		containers = allContainers;
		agents = allAgents;
		if(timeline != null)
			events = timeline.getNodes();
		else
			events = Collections.emptyList();
		// TODO: add agent graph and corresponding representation
	}
	
	@Override
	public boolean start()
	{
		return startSystem();
	}
	
	/**
	 * Starts the whole agent system.
	 * 
	 * @return <code>true</code> in case of success.
	 */
	public boolean startSystem()
	{
		try
		{
			AgentGuiConfig config = new AgentGuiConfig().setWindowType(WINDOW_TYPE).setWindowName(WINDOW_NAME);
			gui = (AgentGui) PlatformUtils.loadClassInstance(this, PlatformUtils.getSimulationGuiClass(), config);
		} catch(Exception e)
		{
			log.error("Unable to create simulation GUI. Simulation stops here." + PlatformUtils.printException(e));
			return false;
		}
		log.info("Simulation Manager started.");
		
		if(!setupGui())
		{
			fullstop();
			return false;
		}
		
		// starts an agent on each platform
		if(!startSimulationAgents())
		{
			fullstop();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Starts the timers associated with the displayed time and the time to the next event.
	 */
	protected void startTimers()
	{
		theTime = new Timer();
		theTime.schedule(new TimerTask() {
			@Override
			public void run()
			{
				time++;
				
				String display = "___" + (int) (time / 600) + ":" + (int) ((time % 600) / 10) + "." + (time % 10)
						+ "___";
				gui.doOutput(SimulationComponent.TIME.toString(),
						new Vector<Object>(Arrays.asList(new Object[] { display })));
				
				int nextEvent = (events.isEmpty() ? 0 : Integer.parseInt(events.get(0).getAttributeValue(
						EVENT_TIME_ATTRIBUTE)));
				while(!events.isEmpty() && (nextEvent <= time * 100))
				{ // there is an event to do
					XMLNode event = events.remove(0);
					log.trace("processing new event");
					
					for(XMLNode task : event.getNodes())
					{
						log.info("task: " + task.getName());
					}
					
					nextEvent = (events.isEmpty() ? 0 : Integer.parseInt(events.get(0).getAttributeValue(
							EVENT_TIME_ATTRIBUTE)));
				}
				if(!events.isEmpty())
					log.info("next event at " + nextEvent);
				else
				{
					log.info("no more events");
					gui.doOutput(SimulationComponent.START.toString(), PlatformUtils.toVector((Object) null));
					gui.doOutput(SimulationComponent.PAUSE.toString(), PlatformUtils.toVector((Object) null));
					gui.doOutput(SimulationComponent.TIME.toString(), PlatformUtils.toVector("no more events"));
					theTime.cancel();
				}
			}
		}, 0, 100);
	}
	
	/**
	 * Sets up the functions of the buttons together with functionality related to simulation time.
	 * 
	 * @return <code>true</code> if setup is successful.
	 */
	protected boolean setupGui()
	{
		gui.connectInput(SimulationComponent.EXIT.toString(), new InputListener() {
			@Override
			public void receiveInput(String componentName, Vector<Object> arguments)
			{
				gui.background(new AgentGuiBackgroundTask() {
					@Override
					public void execute(Object arg, ResultNotificationListener resultListener)
					{
						stop();
						resultListener.receiveResult(null);
					}
				}, null, new ResultNotificationListener() {
					@Override
					public void receiveResult(Object result)
					{
						if(FORCE_SYSTEM_EXIT)
							PlatformUtils.systemExit(0);
					}
				});
			}
		});
		
		if(agents.isEmpty())
		{
			gui.doOutput(SimulationComponent.CREATE.toString(), PlatformUtils.toVector((Object) null));
			gui.doOutput(SimulationComponent.CLEAR.toString(), PlatformUtils.toVector((Object) null));
		}
		else
		{
			gui.connectInput(SimulationComponent.CREATE.toString(), new InputListener() {
				@Override
				public void receiveInput(String componentName, Vector<Object> arguments)
				{
					createAgents();
				}
			});
			gui.connectInput(SimulationComponent.CLEAR.toString(), new InputListener() {
				@Override
				public void receiveInput(String componentName, Vector<Object> arguments)
				{
					signalAllAgents(AgentEventType.AGENT_STOP);
				}
			});
			
		}
		
		gui.connectInput(SimulationComponent.START.toString(), new InputListener() {
			@Override
			public void receiveInput(String componentName, Vector<Object> arguments)
			{
				gui.background(new AgentGuiBackgroundTask() {
					@Override
					public void execute(Object arg, ResultNotificationListener resultListener)
					{
						if(!agentsCreated)
							createAgents();
						signalAllAgents(AgentEventType.SIMULATION_START);
						resultListener.receiveResult(null);
					}
				}, null, new ResultNotificationListener() {
					@Override
					public void receiveResult(Object result)
					{
						if(!events.isEmpty())
						{
							log.info("starting simulation. next event at "
									+ (Integer.parseInt(events.get(0).getAttributeValue("time"))));
							startTimers();
						}
					}
				});
			}
		});
		
		gui.connectInput(SimulationComponent.PAUSE.toString(), new InputListener() {
			@Override
			public void receiveInput(String componentName, Vector<Object> arguments)
			{
				if(events.isEmpty())
				{
					gui.doOutput(SimulationComponent.TIME.toString(), PlatformUtils.toVector("no more events"));
					
					if(!isPaused)
					{
						theTime.cancel();
					}
				}
				else if(isPaused)
				{
					log.info("simulation restarting, next event in "
							+ (Integer.parseInt(events.get(0).getAttributeValue("time")) - time * 100));
					startTimers();
				}
				else
				{
					theTime.cancel();
					log.info("simulation stopped at " + time * 100 + ", next event was in "
							+ (Integer.parseInt(events.get(0).getAttributeValue("time")) - time * 100));
				}
				isPaused = !isPaused;
			}
		});
		
		return true;
	}
	
	/**
	 * Starts the {@link SimulationLinkAgent} instances for all platforms.
	 * 
	 * @return <code>true</code> if the operation succeeded; <code>false</code> otherwise.
	 */
	protected boolean startSimulationAgents()
	{
		for(PlatformLoader platform : platforms.values())
		{
			String platformName = platform.getName();
			MessagingComponent msg = null;
			try
			{
				String msgrClass = platform.getRecommendedComponentClass(AgentComponentName.MESSAGING_COMPONENT);
				if(msgrClass == null)
					msgrClass = AgentComponentName.MESSAGING_COMPONENT.getClassName();
				msg = (MessagingComponent) PlatformUtils.loadClassInstance(this, msgrClass, new Object[0]);
			} catch(Exception e)
			{
				log.error("Failed to create a messaging component for the simulation agent on platform []: []",
						platformName, PlatformUtils.printException(e));
			}
			if(msg != null)
			{
				SimulationLinkAgent agent = new SimulationLinkAgent(SIMULATION_AGENT_NAME_PREFIX + platformName, msg);
				if(!platform.loadAgent(null, agent))
				{
					log.error("Loading simulation agent on platform [" + platformName
							+ "] failed. Simulation cannot start.");
					agent.stop();
					return false;
				}
				if(!agent.start())
				{
					log.error("Starting simulation agent on platform [" + platformName
							+ "] failed. Simulation cannot start.");
					agent.stop();
					return false;
				}
				simulationAgents.put(platformName, agent);
			}
		}
		return true;
	}
	
	/**
	 * Creates all agents in the simulation.
	 */
	protected void createAgents()
	{
		agentsCreated = true;
		
		// load agents on their respective platforms
		Map<String, AgentManager> agentManagers = new HashMap<String, AgentManager>();
		for(AgentCreationData agentData : agents)
		{
			String agentName = agentData.getAgentName();
			if(!platforms.containsKey(agentData.getPlatform()))
			{
				log.error("Platform [" + agentData.getPlatform() + "] for agent [" + agentName + "] not found.");
				continue;
			}
			PlatformLoader platform = platforms.get(agentData.getPlatform());
			String containerName = agentData.getDestinationContainer();
			boolean localContainer = !agentData.isRemote();
			if(localContainer)
			{
				AgentLoader loader = agentData.getAgentLoader();
				AgentManager manager = loader.load(agentData);
				if(manager != null)
					if(platform.loadAgent(containerName, manager))
						agentManagers.put(agentName, manager);
					else
						log.error("agent [" + agentName + "] failed to load on platform [" + platform.getName() + "]");
				else
					log.error("agent [" + agentName + "] failed to load");
			}
			// TODO else: agents in remote containers
		}
		for(Entry<String, AgentManager> agent : agentManagers.entrySet())
		{
			if(agent.getValue().start())
				log.info("agent [" + agent.getKey() + "] started.");
			else
				log.error("agent [" + agent.getKey() + "] failed to start properly.");
		}
		
		// FIXME add await timeout
		// TODO what about agents on other machines?
		boolean stillStarting = true;
		while(stillStarting)
		{
			stillStarting = false;
			for(AgentManager agent : agentManagers.values())
			{
				if(!agent.isRunning())
					stillStarting = true;
			}
			try
			{
				Thread.sleep(50); // FIXME constant timeout in class
			} catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
		// agents started
		for(String platformName : platforms.keySet())
		{
			SimulationLinkAgent simAgent = simulationAgents.get(platformName);
			if(simAgent != null)
				for(AgentCreationData agentData : agents)
					if(agentData.getPlatform().equals(platformName))
						simAgent.enrol(agentData);
		}
	}
	
	/**
	 * Broadcasts the specified event to all agents, via the simulation agents in the respective platforms.
	 * 
	 * @param event
	 *            - the event to broadcast.
	 */
	protected void signalAllAgents(AgentEventType event)
	{
		for(String platformName : platforms.keySet())
			if(simulationAgents.containsKey(platformName))
				simulationAgents.get(platformName).broadcast(event);
	}
	
	@Override
	public boolean stop()
	{
		return stopSystem();
	}
	
	/**
	 * Stops the entire system.
	 * 
	 * @return <code>true</code> in case of success.
	 */
	public boolean stopSystem()
	{
		if(theTime != null)
			theTime.cancel();
		for(SimulationLinkAgent simAgent : simulationAgents.values())
			if(!simAgent.stop())
				log.error("Stopping agent [] failed.", simAgent.getAgentName());
		for(String platformName : platforms.keySet())
			if(!platforms.get(platformName).stop())
				log.error("Stopping platform [] failed.", platformName);
		if(gui != null)
			gui.close();
		if(WindowLayout.staticLayout != null)
			WindowLayout.staticLayout.doexit();
		if(log != null)
			log.doExit();
		return true;
	}
	
	/**
	 * Stops the simulation manager and also exists the application.
	 * <p>
	 * This method should only be called in case of a failed start.
	 * <p>
	 * The system exit is delayed with {@value #SYSTEM_ABORT_DELAY}.
	 * 
	 * @return
	 */
	protected boolean fullstop()
	{
		boolean result = stop();
		new Timer().schedule(new TimerTask() {
			@Override
			public void run()
			{
				PlatformUtils.systemExit(0);
			}
		}, SYSTEM_ABORT_DELAY);
		return result;
	}
	
	/**
	 * As this class implements {@link AgentManager} only for convenience (abusing), it is not expected to be linked to
	 * a platform "above" it, therefore the method will have no effect and always fail.
	 */
	@Override
	public boolean setPlatformLink(PlatformLink link)
	{
		return false;
	}
	
	/**
	 * As this class implements {@link AgentManager} only for convenience (abusing), one can consider it is always
	 * running.
	 */
	@Override
	public boolean isRunning()
	{
		return true;
	}
	
	/**
	 * As this class implements {@link AgentManager} only for convenience (abusing), it does not have an agent name.
	 */
	@Override
	public String getAgentName()
	{
		return null;
	}
}

package example.composite;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * A custom shard that collects all agent events and sends them periodically to another agent.
 * <p>
 * This shard demonstrates:
 * - Extending AgentShardGeneral to access messaging functionality
 * - Collecting events in a list as they occur
 * - Using a Timer to send periodic messages
 * - Using sendMessage() to communicate with other agents via the messaging shard
 * 
 * The shard receives the target agent name via configuration and sends a formatted list
 * of collected events to that agent every 5 seconds.
 * 
 * @author Mario
 */
public class EventCollectorShard extends AgentShardGeneral {
	/**
	 * Serial UID for serialization.
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Parameter name for specifying the target agent to receive event lists.
	 */
	private static final String TARGET_AGENT_PARAMETER = "targetAgent";
	
	/**
	 * Period between sending event lists (in milliseconds).
	 */
	private static final long SEND_PERIOD = 5000;
	
	/**
	 * Endpoint element for this shard (identifies messages from this shard).
	 */
	private static final String SHARD_ENDPOINT = "events";
	
	/**
	 * List to collect all events that occur in the agent.
	 */
	private List<String> eventList;
	
	/**
	 * Timer for periodic sending of collected events.
	 */
	private Timer eventTimer;
	
	/**
	 * Name of the target agent where events will be sent.
	 */
	private String targetAgent;
	
	/**
	 * Counter for events (for tracking).
	 */
	private int eventCounter = 0;
	
	/**
	 * Constructor. Initializes the shard with a custom designation.
	 */
	public EventCollectorShard() {
		super(AgentShardDesignation.customShard("EventCollector"));
		eventList = new ArrayList<>();
	}
	
	/**
	 * Configure the shard by reading the target agent name from deployment parameters.
	 * 
	 * @param configuration - deployment configuration for this shard
	 * @return true if configuration is successful, false otherwise
	 */
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		
		// Get the target agent name where events will be sent
		targetAgent = configuration.getFirstValue(TARGET_AGENT_PARAMETER);
		if(targetAgent == null) {
			li("Warning: targetAgent parameter not specified. Events won't be sent.");
		}
		
		return true;
	}
	
	/**
	 * Entry point for agent event signals. Collects all events and logs them.
	 * 
	 * @param event - the agent event that occurred
	 */
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		
		// Format and collect the event
		String eventDescription = String.format("[%d] %s: %s", 
			++eventCounter, event.getType(), event.toString());
		eventList.add(eventDescription);
		
		li("Event collected: " + eventDescription);
		
		// Log detailed info for AGENT_WAVE (ping messages)
		if(event.getType() == AgentEventType.AGENT_WAVE) {
			AgentWave wave = (AgentWave) event;
			li("Wave received - Content: [" + wave.getContent() + "], Source: [" + 
				wave.getCompleteSource() + "], Destination: [" + wave.getCompleteDestination() + "]");
		}
		
		// Handle startup and shutdown
		if(event.getType() == AgentEventType.AGENT_START) {
			startEventTimer();
		} else if(event.getType() == AgentEventType.AGENT_STOP) {
			stopEventTimer();
		}
	}
	
	/**
	 * Starts the periodic timer that sends collected events to the target agent.
	 * The timer sends events every 5 seconds.
	 */
	private void startEventTimer() {
		li("Starting event collection. Events will be sent to: " + targetAgent);
		eventTimer = new Timer();
		eventTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				sendCollectedEvents();
			}
		}, SEND_PERIOD, SEND_PERIOD);
	}
	
	/**
	 * Stops the periodic timer when the agent shuts down.
	 */
	private void stopEventTimer() {
		if(eventTimer != null) {
			eventTimer.cancel();
			li("Event collection stopped.");
		}
	}
	
	/**
	 * Formats the collected events as a string and sends them to the target agent.
	 * Uses the messaging shard's sendMessage() method for inter-agent communication.
	 */
	private void sendCollectedEvents() {
		if(targetAgent == null || targetAgent.isEmpty()) {
			li("Error: targetAgent not set. Cannot send events.");
			return;
		}
		
		// Log that we're sending events (similar to PingTestShard's "Sending the message....")
		System.out.println("Sending collected events....");
		
		// Format events list as a single string
		StringBuilder eventContent = new StringBuilder();
		eventContent.append("Events collected (").append(eventList.size()).append("): ");
		for(int i = 0; i < eventList.size(); i++) {
			eventContent.append(eventList.get(i));
			if(i < eventList.size() - 1) {
				eventContent.append(" | ");
			}
		}
		
		// Send the formatted event list as a message
		boolean sent = sendMessage(eventContent.toString(), SHARD_ENDPOINT, targetAgent, "events");
		
		if(sent) {
			li("Sent " + eventList.size() + " events to " + targetAgent);
			// Log detail of what was sent
			li("Event list details: " + eventContent.toString());
			// Clear the list after sending so we collect only new events in the next period
			eventList.clear();
		} else {
			li("Failed to send events to " + targetAgent);
		}
	}
}


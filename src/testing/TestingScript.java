package testing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.util.logging.Logger;

/**
 * Represents an entire script, which is a dictionary associating for each agent a list of actions. Each action is an
 * instance of {@link ScriptElement}.
 * 
 * @author Andrei Olaru
 */
public class TestingScript implements Serializable {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 2456818191097124497L;
	
	/**
	 * Indicates how an action is triggered. Triggers may have additional arguments.
	 */
	public enum TriggerType {
		/**
		 * The action is triggered after a given time after the previous action, or, if this is the first action in the
		 * script, after the receipt of the {@link AgentEventType#AGENT_START} event by the testing shard.
		 */
		DELAY,
		
		/**
		 * The action is triggered by the first event that is processed after the completion of the previous action.
		 */
		EVENT,
		
		/**
		 * The action comes immediately after the previous action, before returning the control to the agent for
		 * processing any other agent events.
		 */
		NEXT,
	}
	
	/**
	 * The type of the action. Actions may have arguments. Each action specifies what arguments it can have.
	 */
	public enum ActionType {
		/**
		 * An agent sends a message to another agent.
		 */
		SEND_MESSAGE(new FIELD[] { FIELD.from, FIELD.to, FIELD.with }),
		/**
		 * An agent moves on another node.
		 */
		MOVE_TO_NODE(new FIELD[] { FIELD.to }),
		/**
		 * Prints the message in the <i>with</i> field.
		 */
		PRINT(new FIELD[] { FIELD.with }),
		/**
		 * No action.
		 */
		NOP,
		
		;
		
		/**
		 * The set of arguments allowed by the action.
		 */
		Set<FIELD> argumentsAllowed;
		
		/**
		 * Constructor.
		 */
		private ActionType() {
			this(new FIELD[] {});
		}
		
		/**
		 * Constructor.
		 * 
		 * @param arguments
		 *            - arguments that the action supports.
		 */
		private ActionType(FIELD arguments[]) {
			argumentsAllowed = new HashSet<>(Arrays.asList(arguments));
		}
	}
	
	/**
	 * Arguments allowed by actions. Their meaning is specific to each action.
	 */
	@SuppressWarnings("javadoc")
	public enum FIELD {
		from,
		
		to,
		
		with,
	}
	
	/**
	 * Represents one action in the script.
	 * 
	 * @author Andrei Olaru
	 */
	public static class ScriptElement implements Serializable {
		/**
		 * The serial UID.
		 */
		private static final long serialVersionUID = 972744615563762874L;
		
		/**
		 * Default delay for delay-triggered events which do not specify a delay.
		 */
		public static final int DEFAULT_DELAY = 2000;
		
		TriggerType			trigger	= TriggerType.DELAY;
		String				arg;
		ActionType			action;
		Map<FIELD, String>	arguments;
		
		/**
		 * @return the trigger
		 */
		public TriggerType getTrigger() {
			return trigger;
		}
		
		/**
		 * @param trigger
		 *            the trigger to set
		 */
		public void setTrigger(TriggerType trigger) {
			this.trigger = trigger;
		}
		
		/**
		 * @return the arg
		 */
		public String getArg() {
			return arg;
		}
		
		/**
		 * @param arg
		 *            the arg to set
		 */
		public void setArg(String arg) {
			this.arg = arg;
		}
		
		/**
		 * @return the action
		 */
		public ActionType getAction() {
			return action;
		}
		
		/**
		 * @param action
		 *            the action to set
		 */
		public void setAction(ActionType action) {
			this.action = action;
		}
		
		/**
		 * @return the arguments
		 */
		public Map<FIELD, String> getArguments() {
			return arguments;
		}
		
		/**
		 * @param arguments
		 *            the arguments to set
		 */
		public void setArguments(Map<FIELD, String> arguments) {
			this.arguments = arguments;
		}
		
		public int getDelay() {
			if(trigger != TriggerType.DELAY)
				throw new IllegalStateException("Trigger is not a delay");
			if(arg == null)
				return DEFAULT_DELAY;
			return Integer.parseInt(arg);
		}
		
		public AgentEventType getEvent() {
			if(trigger != TriggerType.EVENT)
				throw new IllegalStateException("Trigger is not an event");
			return AgentEventType.valueOf(arg);
		}
		
		public boolean verifyElement(Logger log) {
			if(action == null)
				return log.ler(false, "No action specified [].", this);
			if(arguments != null)
				for(FIELD field : arguments.keySet())
					if(!action.argumentsAllowed.contains(field))
						return log.ler(false, "Action argument [] not allowed for action [].", field, action);
			return true;
		}
		
		@Override
		public String toString() {
			return action + " [" + trigger + " : " + arg + "] " + arguments;
		}
	}
	
	public static class AgentScript implements Serializable {
		private static final long	serialVersionUID	= 9164860342311455468L;
		List<ScriptElement>			actions;
		
		/**
		 * @return the script
		 */
		public List<ScriptElement> getActions() {
			return actions;
		}
		
		/**
		 * @param script
		 *            the script to set
		 */
		public void setActions(List<ScriptElement> script) {
			this.actions = script;
		}
		
		public boolean verify(Logger log) {
			for(ScriptElement element : actions)
				if(!element.verifyElement(log))
					return false;
			return true;
		}
	}
	
	Map<String, AgentScript> script;
	
	/**
	 * @return the script
	 */
	public Map<String, AgentScript> getScript() {
		return script;
	}
	
	/**
	 * @param script
	 *            the script to set
	 */
	public void setScript(Map<String, AgentScript> script) {
		this.script = script;
	}
	
	public boolean verify(Logger log) {
		for(AgentScript agentScript : script.values())
			for(ScriptElement element : agentScript.actions)
				if(!element.verifyElement(log))
					return false;
		return true;
	}
}
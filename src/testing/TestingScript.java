package testing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.util.logging.Logger;

public class TestingScript implements Serializable {
	public enum TriggerType {
		DELAY,
		
		EVENT,
	}
	
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
		 * No action.
		 */
		NOP,
		
		;
		
		Set<FIELD> argumentsAllowed;
		
		private ActionType() {
			this(new FIELD[] {});
		}
		
		private ActionType(FIELD arguments[]) {
			argumentsAllowed = new HashSet<>(Arrays.asList(arguments));
		}
	}
	
	public enum FIELD {
		from,
		
		to,
		
		with,
	}
	
	public static class ScriptElement implements Serializable {
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
		List<ScriptElement> actions;
		
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
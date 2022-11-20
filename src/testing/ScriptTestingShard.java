package testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import org.yaml.snakeyaml.Yaml;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent.MobileCompositeAgentShardContainer;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import testing.TestingScript.ActionType;
import testing.TestingScript.AgentScript;
import testing.TestingScript.FIELD;
import testing.TestingScript.ScriptElement;
import testing.TestingScript.TriggerType;

/**
 * A shard built for scripted testing. It uses an YAML file that lists the actions to do.
 * <p>
 * Each action can have a trigger -- an agent event or a delay relative to the previous action. The script begins
 * execution at the {@link AgentEventType#AGENT_START} event.
 * <p>
 * FIXME: if execution is interrupted (e.g. when the agent migrates) and the next action is triggered by a delay, the
 * timer will be reset after resuming execution, instead of continuing with the time left.
 * <p>
 * <b>Important Note:</b> it is assumed that this shard runs within an agent similar to {@link CompositeAgent}, in which
 * the handling of an event must be completed before the next event is handled. Hence, there will be no concurrent calls
 * of {@link #signalAgentEvent(AgentEvent)}.
 * 
 * @author Andrei Olaru
 * @author Monica Pricope
 */
public class ScriptTestingShard extends AgentShardGeneral {
	/**
	 * The timer task implementation for delayed actions.
	 */
	abstract class ScriptTimerTask extends TimerTask {
		/**
		 * The action to perform when the timer is activated
		 */
		ScriptElement	theAction;
		/**
		 * Indicates the action after that is triggered by a delay.
		 */
		boolean			nextIsDelayed;
		
		/**
		 * The constructor.
		 * 
		 * @param action
		 *            - the action to perform.
		 * @param nextDelayed
		 *            - indicates the action after that is triggered by a delay.
		 */
		public ScriptTimerTask(ScriptElement action, boolean nextDelayed) {
			theAction = action;
			nextIsDelayed = nextDelayed;
		}
	}
	
	/**
	 * The serial UID.
	 */
	private static final long	serialVersionUID	= -3151844526556248974L;
	/**
	 * Shard designation.
	 */
	public static final String	DESIGNATION			= "test/script";
	/**
	 * The parameter that indicates the file to get the script from.
	 */
	public static final String	FROM_PARAMETER		= "from";
	
	/**
	 * The entire testing script, which may include actions for other entities.
	 */
	protected TestingScript			entireScript;
	/**
	 * The script for this agent.
	 */
	protected List<ScriptElement>	agentScript;
	
	/**
	 * Timer for delayed actions.
	 */
	transient Timer											actionTimer				= null;
	/**
	 * If the next action is triggered by a delay, this field contains the action. This field and {@link #actionTimer}
	 * must be <code>null</code> simultaneously.
	 */
	ScriptElement											delayTriggeredAction	= null;
	/**
	 * If the next action is triggered by an event, this field contains all the necessary information.
	 */
	AbstractMap.SimpleEntry<AgentEventType, ScriptElement>	eventTriggeredAction	= null;
	/**
	 * If <code>true</code>, no further attempts to schedule actions are performed.
	 */
	boolean													scriptCompleted			= false;
	/**
	 * If <code>true</code>, calls to {@link #scheduleNextAction(AgentEvent)} should not schedule any actions, because
	 * the script is waiting for the completion of a delay, before scheduling any further actions.
	 * <p>
	 * It effectively acts as a semaphore for the scheduling of following events.
	 */
	boolean													nextActionScheduled		= false;
	
	/**
	 * No-argument constructor
	 */
	public ScriptTestingShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(!configuration.isSimple(FROM_PARAMETER))
			return false;
		scriptCompleted = true;
		String file = configuration.getAValue(FROM_PARAMETER);
		List<String> paths = new LinkedList<>();
		String filename = Loader.autoFind(configuration.getValues(CategoryName.PACKAGE.s()), null, file, null,
				"script.yaml", paths);
		if(filename == null)
			return ler(false, "Script file cannot be found for script []. Tried paths: ", file, paths);
		try (FileInputStream input = new FileInputStream(new File(filename))) {
			entireScript = new Yaml().loadAs(input, TestingScript.class);
		} catch(FileNotFoundException e) {
			return ler(false, "Cannot load file [].", file);
		} catch(IOException e1) {
			return ler(false, "File close error for file [].", file);
		} catch(Exception e) {
			return ler(false, "Script load failed from [] with []", file, e);
		}
		// if(!entireScript.verify(getLogger()))
		// le("Script [] loaded from [] but failed verification.", file, filename);
		// li("Script loaded and checked: [] from [].", file, filename); // commented because it will set unit name
		return true;
	}
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		if(getAgent() != null) {
			if(entireScript == null || entireScript.script == null
					|| !entireScript.script.containsKey(getAgent().getEntityName()))
				li("No script for agent", getAgent().getEntityName());
			else {
				AgentScript script = entireScript.script.get(getAgent().getEntityName());
				if(script.verify(getLogger()))
					lf("Script verification ok");
				agentScript = script.getActions();
				if(agentScript != null && !agentScript.isEmpty())
					scriptCompleted = false;
				else
					lf("No script for agent", getAgent().getEntityName());
			}
		}
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		boolean dontScheduleNext = false;
		switch(event.getType()) {
		case AGENT_START:
			nextActionScheduled = false;
			scheduleNextAction(event);
			break;
		case AGENT_STOP:
			dontScheduleNext = true;
			if(actionTimer != null) {
				actionTimer.cancel();
				actionTimer = null;
				// delayTriggeredAction.setArg([get remaining time]);
				agentScript.add(0, delayTriggeredAction);
			}
			break;
		default:
			// nothing to do
		}
		
		if(eventTriggeredAction != null && eventTriggeredAction.getKey().equals(event.getType())) {
			ScriptElement a = eventTriggeredAction.getValue();
			eventTriggeredAction = null;
			nextActionScheduled = false;
			performAction(a, dontScheduleNext);
		}
	}
	
	/**
	 * Schedules the next action, depending on its trigger or, if the case, performs the next action.
	 * 
	 * @param startEvent
	 *            - the event that lead to the call of this method, if any (should only be
	 *            {@link AgentEventType#AGENT_START}.
	 */
	protected void scheduleNextAction(AgentEvent startEvent) {
		if(scriptCompleted || nextActionScheduled)
			return;
		boolean isNextActionDelayed = false;
		ScriptElement a = null;
		synchronized(agentScript) {
			if(!agentScript.isEmpty()) {
				a = agentScript.remove(0);
				isNextActionDelayed = agentScript.isEmpty() ? false
						: TriggerType.DELAY.equals(agentScript.get(0).getTrigger());
				// any further actions in the script must wait for the delay
				nextActionScheduled = true;
			}
		}
		if(a == null) {
			lf("Script completed.");
			scriptCompleted = true;
			return;
		}
		switch(a.getTrigger()) {
		case DELAY:
			// next action is scheduled
			actionTimer = new Timer();
			long delay;
			delay = a.getDelay();
			delayTriggeredAction = a;
			actionTimer.schedule(new ScriptTimerTask(a, isNextActionDelayed) {
				@Override
				public void run() {
					actionTimer = null;
					delayTriggeredAction = null;
					nextActionScheduled = false;
					if(nextIsDelayed && !ActionType.MOVE_TO_NODE.equals(theAction.action))
						// only scheduled next delayed action if the agent is not going to move
						scheduleNextAction(null);
					performAction(theAction, false);
				}
			}, delay);
			break;
		case EVENT:
			// next action is scheduled
			eventTriggeredAction = new AbstractMap.SimpleEntry<>(a.getEvent(), a);
			break;
		case NEXT:
			nextActionScheduled = false;
			performAction(a, false);
			break;
		default:
			le("Unknown trigger []", a.getTrigger());
		}
	}
	
	/**
	 * Performs the next action in the script.
	 * <p>
	 * It also calls for the scheduling of the next action, if any action can be scheduled.
	 * 
	 * @param action
	 *            - the action to perform.
	 * @param dontScheduleNext
	 *            - if <code>true</code>, {@link #scheduleNextAction} will not be called after performing the action.
	 */
	protected void performAction(ScriptElement action, boolean dontScheduleNext) {
		lf("Script activate action []. Rest of actions:", action,
				agentScript.stream().map(a -> a.getAction()).collect(Collectors.toList()));
		Map<FIELD, String> args = action.arguments;
		switch(action.action) {
		case SEND_MESSAGE:
			// FIXME check if all fields are present
			sendMessage(args.get(FIELD.with), null, args.get(FIELD.to));
			break;
		case MOVE_TO_NODE:
			// FIXME check if all fields are present
			if(getAgent() instanceof MobileCompositeAgentShardContainer) {
				((MobileCompositeAgentShardContainer) getAgent()).moveTo(args.get(FIELD.to));
			}
			else
				le("Agent is not mobile.");
			break;
		case PRINT:
			li("ECHO ", args.get(FIELD.with));
			break;
		case NOP:
			break;
		default:
			le("Unknown action: ", action);
			break;
		}
		if(!dontScheduleNext)
			scheduleNextAction(null);
	}
}

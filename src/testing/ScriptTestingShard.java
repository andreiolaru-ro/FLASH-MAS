package testing;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent.MobileCompositeAgentShardContainer;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.MultiTreeMap;
import testing.TestingScript.AgentScript;
import testing.TestingScript.FIELD;
import testing.TestingScript.ScriptElement;
import testing.TestingScript.TriggerType;

/**
 * @author Andrei Olaru
 * @author Monica Pricope
 */
public class ScriptTestingShard extends AgentShardGeneral {
	/**
	 * The serial UID.
	 */
	private static final long	serialVersionUID	= -3151844526556248974L;
	/**
	 * Shard designation.
	 */
	public static final String	DESIGNATION			= "test/script";
	public static final String	FROM_PARAMETER		= "from";
	
	protected TestingScript			entireScript;
	protected List<ScriptElement>	agentScript;
	
	transient Timer	action_timer	= null;
	Integer			savedTime		= null;
	
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
				agentScript = script.actions;
			}
		}
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			scheduleNextAction(event);
			break;
		case AGENT_STOP:
			if(action_timer != null && TriggerType.DELAY.equals(getNextAction().getTrigger()))
				// don't know how (if possible) to get remaining time
				savedTime = Integer.valueOf(getNextAction().getDelay());
			if(action_timer != null)
				action_timer.cancel();
			action_timer = null;
			break;
		default:
			break;
		}
		if(getNextAction() != null && TriggerType.EVENT.equals(getNextAction().getTrigger())
				&& event.getType().equals(getNextAction().getEvent()))
			performNextAction();
	}
	
	protected void scheduleNextAction(AgentEvent event) {
		if(getNextAction() == null) {
			li("Script completed.");
			return;
		}
		if(TriggerType.DELAY.equals(getNextAction().getTrigger())) {
			action_timer = new Timer();
			long delay;
			if(event != null && event.isSet(CompositeAgent.TRANSIENT_EVENT_PARAMETER) && savedTime != null
					&& TriggerType.DELAY.equals(getNextAction().getTrigger()))
				delay = savedTime.longValue();
			else
				delay = getNextAction().getDelay();
			action_timer.schedule(new TimerTask() {
				@Override
				public void run() {
					performNextAction();
					action_timer = null;
				}
			}, delay);
		}
	}
	
	protected ScriptElement getNextAction() {
		if(agentScript == null || agentScript.isEmpty())
			return null;
		return agentScript.get(0);
	}
	
	protected void performNextAction() {
		ScriptElement action = getNextAction();
		agentScript.remove(0);
		lf("Script activate action []. Rest of actions:", action,
				agentScript.stream().map(a -> a.getAction()).collect(Collectors.toList()));
		scheduleNextAction(null);
		Map<FIELD, String> args = action.arguments;
		switch(action.action) {
		case SEND_MESSAGE:
			sendMessage(args.get(FIELD.with), null, args.get(FIELD.to));
			break;
		case MOVE_TO_NODE:
			if(getAgent() instanceof MobileCompositeAgentShardContainer)
				((MobileCompositeAgentShardContainer) getAgent()).moveTo(args.get(FIELD.to));
			else
				le("Agent is not mobile.");
			break;
		case NOP:
			break;
		default:
			le("Unknown action: ", action);
			break;
		}
	}
}

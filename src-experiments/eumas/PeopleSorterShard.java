package eumas;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;

import org.yaml.snakeyaml.Yaml;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

@SuppressWarnings("javadoc")
public class PeopleSorterShard extends AgentShardGeneral {
	
	private static final long		serialVersionUID	= 1L;
	LinkedHashMap<Integer, String>	script;
	Timer							timer				= null;
	int								item				= 1;
	MLDriver						mlDriver			= null;
	
	public PeopleSorterShard() {
		super(AgentShardDesignation.customShard("Sorter"));
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		String file = configuration.getAValue("script");
		// List<String> paths = new LinkedList<>();
		// String filename = Loader.autoFind(configuration.getValues(CategoryName.PACKAGE.s()), null, "script", file,
		// ".yaml", paths);
		// if(filename == null)
		// return ler(false, "Script file cannot be found for script []. Tried paths: ", file, paths);
		try (FileInputStream input = new FileInputStream(new File("src-experiments/eumas/People.yaml"))) {
			script = new Yaml().loadAs(input, LinkedHashMap.class);
		} catch(FileNotFoundException e) {
			return ler(false, "Cannot load file [].", file, e);
		} catch(IOException e) {
			return ler(false, "File close error for file [].", file, e);
		} catch(Exception e) {
			return ler(false, "Script load failed from [] with []", file, e);
		}
		li("Scenario script loaded");
		return true;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!super.addGeneralContext(context))
			return false;
		if(context instanceof MLDriver) {
			mlDriver = (MLDriver) context;
			li("ML Driver detected");
			return true;
		}
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			timer = new Timer();
			scheduleTimer();
			break;
		case AGENT_STOP:
			timer.cancel();
			break;
		default:
			break;
		}
	}
	
	protected void scheduleTimer() {
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				runTask();
			}
		}, 2000);
	}
	
	@SuppressWarnings("unchecked")
	protected void runTask() {
		if(!script.containsKey(Integer.valueOf(item))) {
			timer.cancel();
			li("Script done");
			getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
			return;
		}
		
		String image = script.get(Integer.valueOf(item)), filename = MLDriver.ML_DIRECTORY_PATH + "input/" + image;
		RemoteOperationShard remote = ((RemoteOperationShard) getAgentShard(
				AgentShardDesignation.standardShard(StandardAgentShard.REMOTE)));
		remote.sendOutput(new AgentWave(image, "current-image"));
		remote.sendOutput(new AgentWave(Integer.valueOf(item).toString(), "nprocessed"));
		item++;
		ArrayList<Object> result = mlDriver.predict("YOLOv8-pedestrians", filename, false);
		Double number = ((ArrayList<Double>) result.get(0)).get(0);
		li("Prediction result: [] / []", result, number);
		
		String targetAgent = null;
		if(number.doubleValue() == 0)
			targetAgent = "NoPeople";
		else if(number.doubleValue() < 5)
			targetAgent = "FewPeople";
		else
			targetAgent = "MorePeople";
		getMessagingShard().sendMessage(new AgentWave(image, targetAgent));
		
		scheduleTimer();
	}
}

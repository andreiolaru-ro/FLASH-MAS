package aifolk.scenario;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.yaml.snakeyaml.Yaml;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ml.MLDriver;

public class ScenarioDriver extends EntityCore<Node> implements EntityProxy<ScenarioDriver> {
	
	Map<String, ScenarioShard>	agents	= new HashMap<>();
	LinkedHashMap<Integer, String>	script;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		String file = configuration.getAValue("script");
		List<String> paths = new LinkedList<>();
		String filename = Loader.autoFind(configuration.getValues(CategoryName.PACKAGE.s()), null, "script", file,
				".yaml", paths);
		if(filename == null)
			return ler(false, "Script file cannot be found for script []. Tried paths: ", file, paths);
		try (FileInputStream input = new FileInputStream(new File(filename))) {
			script = new Yaml().loadAs(input, LinkedHashMap.class);
		} catch(FileNotFoundException e) {
			return ler(false, "Cannot load file [].", file);
		} catch(IOException e1) {
			return ler(false, "File close error for file [].", file);
		} catch(Exception e) {
			return ler(false, "Script load failed from [] with []", file, e);
		}
		li("Scenario script loaded");
		return true;
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		li("Scenario driver up");
		
		String agent = "A";
		
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			int step = 0;
			
			@SuppressWarnings("synthetic-access")
			@Override
			public void run() {
				if(script.containsKey(Integer.valueOf(step))) {
					String file = script.get(Integer.valueOf(step));
					agents.get(agent).initiateAgentEvent(MLDriver.ML_DIRECTORY_PATH + "input/" + file, step);
					step++;
				}
				else {
					timer.cancel();
					li("Script done");
				}
			}
		}, 4000, 2000);
		
		return true;
	}
	
	public boolean registerAgent(String agentName, ScenarioShard scenarioShard) {
		agents.put(agentName, scenarioShard);
		li("Registered agent [] with shard [].", agentName, scenarioShard);
		return true;
	}
	
	public void receiveAgentOutput(Object output, long ID) {
		li("Obtained output for ID ", ID);
		// TODO evaluate
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		// TODO
		// send scenario termination events via scenario shards
		li("Scenario driver stopped");
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<ScenarioDriver> asContext() {
		return this;
	}
	
	@Override
	public String getEntityName() {
		return getName();
	}
	
}

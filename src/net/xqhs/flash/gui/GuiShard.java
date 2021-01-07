package net.xqhs.flash.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.IOShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.gui.structure.Element;

public class GuiShard extends IOShard {
	/**
	 * The UID.
	 */
	private static final long serialVersionUID = -2769555908800271606L;
	
	protected Element interfaceStructure;
	
	public GuiShard() {
		super(StandardAgentShard.GUI.toAgentShardDesignation());
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		super.configure(configuration);
		String config = configuration.getFirstValue("from");
		if(config != null) {
			if(config.endsWith(".yml") || config.endsWith(".yaml")) { // file
				List<String> checked = new LinkedList<>();
				String path = Loader.autoFind(configuration.getValues(CategoryName.PACKAGE.s()), config, null, null,
						null, checked);
				if(path == null) {
					le("Cannot find file []. Check paths: ", config, checked);
					return false;
				}
				try (FileInputStream input = new FileInputStream(new File(path))) {
					interfaceStructure = new Yaml().loadAs(input, Element.class);
				} catch(FileNotFoundException e) {
					le("Cannot load file [].", config);
					return false;
				} catch(IOException e1) {
					le("File close error for file [].", config);
					return false;
				} catch(Exception e) {
					le("Interface load failed from [] with []", config, e);
					e.printStackTrace();
					return false;
				}
			}
			else // inline
				try {
					interfaceStructure = new Yaml().loadAs(config, Element.class);
				} catch(Exception e) {
					le("Interface load failed from [] with []", config, e);
					e.printStackTrace();
					return false;
				}
			if(interfaceStructure == null) {
				le("Interface load failed from []", config);
				return false;
			}
		}
		
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		// if(event.getType() == AgentEventType.AGENT_START)
		// ((MonitoringShard) getAgentShard(StandardAgentShard.MONITORING.toAgentShardDesignation()))
		// .sendGuiUpdate(new Yaml().dump(interfaceStructure));
	}
	
	@Override
	public AgentWave getInput(String portName) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public void sendOutput(AgentWave agentWave) {
		// TODO Auto-generated method stub
		
	}
}

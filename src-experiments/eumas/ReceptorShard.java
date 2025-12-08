package eumas;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

@SuppressWarnings("javadoc")
public class ReceptorShard extends AgentShardGeneral {
	
	private static final long serialVersionUID = 1L;
	
	List<String> images = new LinkedList<>();
	
	public ReceptorShard() {
		super(AgentShardDesignation.customShard("Receptor"));
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		if(event.getType() != AgentEventType.AGENT_WAVE)
			return;
		String content = event.get(AgentWave.CONTENT);
		// String content = "here";
		li("received content:", content);
		
		RemoteOperationShard remote = ((RemoteOperationShard) getAgentShard(
				AgentShardDesignation.standardShard(StandardAgentShard.REMOTE)));
		if(content.equals("do stop"))
			getAgent().postAgentEvent(new AgentEvent(AgentEventType.AGENT_STOP));
		else if(content.equals("inactivate")) {
			remote.sendOutput(new AgentWave("inactive", "received"));
			remote.sendOutput(new AgentWave("inactive.jpg", "latest"));
		}
		else {
			images.add(content);
			remote.sendOutput(new AgentWave(images.stream().collect(Collectors.joining("\n")), "received"));
			remote.sendOutput(new AgentWave("2/" + content, "latest"));
			
			Element element = new Element();
			element.setPort("image");
			element.setType("label");
			element.setRole("img");
			element.setValue(content);
			remote.addGuiElementToContainer("images", element);
		}
	}
}

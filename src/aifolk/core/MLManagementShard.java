/**
 * 
 */
package aifolk.core;

import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aifolk.onto.OntologyDriver;
import aifolk.onto.Query;
import aifolk.onto.QueryResult;
import aifolk.scenario.ScenarioShard;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.ml.MLDriver;

/**
 * Shard to manage the use of ML models, according to the AI Folk methodology.
 */
public class MLManagementShard extends AgentShardGeneral {
	/**
	 * The serial UID
	 */
	private static final long		serialVersionUID	= 3728868349364945506L;
	/**
	 * {@link AgentShardDesignation}.
	 */
	private static final String		DESIGNATION			= "ML:Management";
	
	/**
	 * The node-local {@link MLDriver} instance.
	 */
	MLDriver		mlDriver;
	/**
	 * The node-local {@link OntologyDriver} instance.
	 */
	OntologyDriver	ontDriver;
	
	/**
	 * The constructor.
	 */
	public MLManagementShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
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
		if(context instanceof OntologyDriver) {
			ontDriver = (OntologyDriver) context;
			li("Ontology Driver detected");
			return true;
		}
		return false;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		// TODO if event is output from agent ML pipeline, evaluate and decide
		
		if(!AgentEventType.AGENT_WAVE.equals(event.getType()) || !(event instanceof AgentWave))
			return;
		
		if(ScenarioShard.DESIGNATION.equals(event.getValue(AgentWave.SOURCE_ELEMENT))) {
			// it is an input for the ML pipeline
			// TODO recognize situation -- run scripts, evaluate props of current input window
			
			// situation element: no of pedestrians
			li("Result:", mlDriver.predict("YOLOv8-pedestrians", (String) event.getObject("input"), false));
			// TODO match with current model description
			// TODO if insufficient match, build query
			// TODO send query to other agents
		}
		else {
			List<String> sources = event.getValues(AgentWave.SOURCE_ELEMENT);
			if(sources.size() > 2 && !getAgent().getEntityName().equals(sources.get(0))
					&& DESIGNATION.equals(sources.get(1))) {
				// wave is from the MLManagementShard of a different agent
				JsonObject jsonObj = JsonParser.parseString(event.get(AgentWave.CONTENT)).getAsJsonObject();
				String messageType = jsonObj.get(AIFolkProtocol.FOLK_PROTOCOL).getAsString();
				JsonArray arguments = jsonObj.get(AIFolkProtocol.FOLK_ARGUMENTS).getAsJsonArray();
				switch(messageType) {
				case AIFolkProtocol.FOLK_SEARCH:
					String queryString = arguments.get(0).getAsString();
					QueryResult[] results = ontDriver.runQuery(new Query(queryString));
					AgentWave reply = ((AgentWave) event).createReply(new Gson().toJson(results));
					sendMessage(reply);
					break;
				// TODO
				default:
					le("Unknown protocol []", messageType);
				}
			}
		}
		
	}
}

/**
 * 
 */
package aifolk.core;

import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import aifolk.onto.OntologyDriver;
import aifolk.onto.Query;
import aifolk.onto.QueryResult;
import aifolk.onto.vocab.CoreVocabulary;
import aifolk.onto.vocab.DrivingSceneContextDescription;
import aifolk.onto.vocab.ExportableDescription;
import aifolk.onto.vocab.ExtractableDescription;
import aifolk.onto.vocab.ModelDescription;
import aifolk.onto.vocab.TaskDescription;
import aifolk.scenario.ScenarioShard;
import fr.inria.corese.core.Graph;
import fr.inria.corese.core.load.LoadException;
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
	public boolean addGeneralContext(final EntityProxy<? extends Entity<?>> context) {
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
	public void signalAgentEvent(final AgentEvent event) {
		super.signalAgentEvent(event);
		// TODO if event is output from agent ML pipeline, evaluate and decide
		
		if(!AgentEventType.AGENT_WAVE.equals(event.getType()) || !(event instanceof AgentWave))
			return;
		
		if(ScenarioShard.DESIGNATION.equals(event.getValue(AgentWave.SOURCE_ELEMENT))) {
			// it is an input for the ML pipeline
			// TODO recognize situation -- run scripts, evaluate props of current input window
			
			// situation element: no of pedestrians
			li("Result:", mlDriver.predict("YOLOv8-pedestrians", (String) event.getObject("input"), false));
			final DrivingSceneContextDescription.Builder dataContextBuilder = DrivingSceneContextDescription.Builder.create("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#my_data_context_1");
			dataContextBuilder.withAvgNumPedestrians(150);
			final DrivingSceneContextDescription dataContextDesc = dataContextBuilder.build();
			
			final TaskDescription.Builder taskBuilder = TaskDescription.Builder.create("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#my_task_1", 
										"http://aimas.cs.pub.ro/ai-folk/ontology/core#SemanticSegmentation");
			taskBuilder.setDomainURI("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#AutonomousDriving");
			taskBuilder.addDataContextDescription(dataContextDesc);
			final TaskDescription taskDesc = taskBuilder.build();

			// TODO send task description to other agents by first getting a String serialization of it
			final Optional<String> taskDescStr = ExportableDescription.graphToString(taskDesc.exportToGraph());

			// Parse the serialized task description to obtain a task description object
			try {
				final Graph taskDescGraph = ExtractableDescription.getGraphFromString(taskDescStr.get());
				final String taskDescInstanceURI = ExtractableDescription.getSingleConceptURI(taskDescGraph, CoreVocabulary.TASK_CHARACTERIZATION.stringValue());
				final TaskDescription extractedTaskDesc = new TaskDescription(taskDescGraph, taskDescInstanceURI);
				extractedTaskDesc.populateDescription(false);

				final ModelDescription modelDesc = ModelDescription.getFromFile("");
				modelDesc.populateDescription(true);
				((DrivingSceneContextDescription)modelDesc.getModelEvaluations().get(0).getDatasetDescription().getDataContextDescriptions().get(0)).getAvgNumPedestrians();

			} catch (final LoadException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// TODO match with current model description
			// TODO if insufficient match, build query
			// TODO send query to other agents
		}
		else {
			final List<String> sources = event.getValues(AgentWave.SOURCE_ELEMENT);
			if(sources.size() > 2 && !getAgent().getEntityName().equals(sources.get(0))
					&& DESIGNATION.equals(sources.get(1))) {
				// wave is from the MLManagementShard of a different agent
				final JsonObject jsonObj = JsonParser.parseString(event.get(AgentWave.CONTENT)).getAsJsonObject();
				final String messageType = jsonObj.get(AIFolkProtocol.FOLK_PROTOCOL).getAsString();
				final JsonArray arguments = jsonObj.get(AIFolkProtocol.FOLK_ARGUMENTS).getAsJsonArray();
				switch(messageType) {
				case AIFolkProtocol.FOLK_SEARCH:
					final String queryString = arguments.get(0).getAsString();
					final QueryResult[] results = ontDriver.runQuery(new Query(queryString));
					final AgentWave reply = ((AgentWave) event).createReply(new Gson().toJson(results));
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

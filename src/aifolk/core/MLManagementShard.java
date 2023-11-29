/**
 * 
 */
package aifolk.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.Gson;

import aifolk.onto.OntologyDriver;
import aifolk.onto.Query;
import aifolk.onto.QueryResult;
import aifolk.onto.vocab.DrivingSceneContextDescription;
import aifolk.onto.vocab.ExportableDescription;
import aifolk.onto.vocab.ModelDescription;
import aifolk.onto.vocab.TaskDescription;
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
	static class DataContext {
		// nothing to specify
	}
	
	/**
	 * The serial UID
	 */
	private static final long	serialVersionUID	= 3728868349364945506L;
	/**
	 * {@link AgentShardDesignation}.
	 */
	private static final String	DESIGNATION			= "ML:Management";
	
	/**
	 * The node-local {@link MLDriver} instance.
	 */
	MLDriver		mlDriver;
	/**
	 * The node-local {@link OntologyDriver} instance.
	 */
	OntologyDriver	ontDriver;
	DataContext		currentDataContext	= new DrivingDataContext();
	
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
			// recognize situation -- run scripts, evaluate props of current input window
			
			// situation element: no of pedestrians
			ArrayList<Object> result = mlDriver.predict("YOLOv8-pedestrians", (String) event.getObject("input"), false);
			li("Result:", result);
			((DrivingDataContext) currentDataContext)
					.addPedestrianNumberData(Long.valueOf(Math.round(((Double) result.get(0)).doubleValue())));
			
			// match with current model description
			ModelDescription modelDesc = new ModelDescription(ontDriver.getGraph(),
					"http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#model_node_1");
			modelDesc.populateDescription(true);
			Integer pedestriansInModel = ((DrivingSceneContextDescription) modelDesc.getModelEvaluations().get(0)
					.getDatasetDescription()
					.getDataContextDescriptions().get(0)).getAvgNumPedestrians().get();
			li("pedestrians in model:", pedestriansInModel);
			//
			// Graph taskDescGraph = ontDriver.getGraph();
			// String taskDescInstanceURI = ExtractableDescription.getSingleConceptURI(taskDescGraph,
			// "http://aimas.cs.pub.ro/ai-folk/mlmodels/model_node_1");
			// TaskDescription extractedTaskDesc = new TaskDescription(taskDescGraph, taskDescInstanceURI);
			// extractedTaskDesc.populateDescription(false);
			// // access the get average num pedestrians
			// Integer pedestriansInModel = ((DrivingSceneContextDescription) extractedTaskDesc
			// .getDataContextDescriptions().get(0)).getAvgNumPedestrians().get();
			
			// Integer pedestriansInModel = Integer.valueOf(1);
			
			if(pedestriansInModel.intValue() < ((DrivingDataContext) currentDataContext).getMaxPedestrians()
					.intValue()) {
				// time to change model
				li("must change model");
				final DrivingSceneContextDescription.Builder dataContextBuilder = DrivingSceneContextDescription.Builder
						.create("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#my_data_context_1");
				dataContextBuilder.withAvgNumPedestrians(150);
				final DrivingSceneContextDescription dataContextDesc = dataContextBuilder.build();
				
				final TaskDescription.Builder taskBuilder = TaskDescription.Builder.create(
						"http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#my_task_1",
						"http://aimas.cs.pub.ro/ai-folk/ontology/core#SemanticSegmentation");
				taskBuilder
						.setDomainURI("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#AutonomousDriving");
				taskBuilder.addDataContextDescription(dataContextDesc);
				final TaskDescription taskDesc = taskBuilder.build();
				
				// TODO send task description to other agents by first getting a String serialization of it
				final Optional<String> taskDescStr = ExportableDescription.graphToString(taskDesc.exportToGraph());
				// FIXME check if optional ok
				// FIXME the list of friends is fixed
				AgentWave message = (AgentWave) new AgentWave(taskDescStr.get()).addSourceElements(DESIGNATION)
						.add(AIFolkProtocol.FOLK_PROTOCOL, AIFolkProtocol.FOLK_SEARCH);
				message.resetDestination("B", DESIGNATION);
				sendMessage(message);
			}
			
			// TODO send query to other agents
		}
		else {
			// FIXME alternatively, may have used endpoints such as A/ML-Management/<protocol>
			List<String> sources = event.getValues(AgentWave.SOURCE_ELEMENT);
			if(sources.size() >= 2 && !getAgent().getEntityName().equals(sources.get(0))
					&& DESIGNATION.equals(sources.get(1))) {
				// wave is from the MLManagementShard of a different agent
				String messageType = event.get(AIFolkProtocol.FOLK_PROTOCOL);
				String query = event.get(AgentWave.CONTENT);
				switch(messageType) {
				case AIFolkProtocol.FOLK_SEARCH: {
					lf("received [] for []", messageType, query);
					
					QueryResult[] results = ontDriver.runQuery(new Query(query));
					AgentWave reply = ((AgentWave) event).createReply(new Gson().toJson(results));
					// TODO add protocol
					
					sendMessage(reply);
					break;
				}
				case AIFolkProtocol.FOLK_LISTING: {
//					List<QueryResult> results = new ArrayList<>();
//					for(int i = 0; i < arguments.size(); i++) {
//						results.add(new Gson().fromJson(arguments.get(i), QueryResult.class));
//						// TODO manage result
// }
					break;
				}
				case AIFolkProtocol.FOLK_REQUEST: {
					String modelID = event.get(AgentWave.CONTENT);
					// TODO check with MLDriver if we have the model
					ModelDescription model = null;
					AgentWave reply = ((AgentWave) event).createReply(new Gson().toJson(model));
					reply.add(AIFolkProtocol.FOLK_PROTOCOL, AIFolkProtocol.FOLK_TRANSFER);
					sendMessage(reply);
					break;
				}
				case AIFolkProtocol.FOLK_TRANSFER: {
					ModelDescription model = new Gson().fromJson(event.get(AgentWave.CONTENT), ModelDescription.class);
					// TODO add model description to local graph
					break;
				}
				default:
					le("Unknown protocol []", messageType);
				}
			}
		}
		
	}
}

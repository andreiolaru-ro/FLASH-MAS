package aifolk.scenario;

import java.util.ArrayList;

import aifolk.core.DrivingDataContext;
import aifolk.core.MLManagementShard.DataContext;
import aifolk.core.MLManagementShard.Feature;
import aifolk.onto.OntologyDriver;
import aifolk.onto.vocab.DrivingSceneContextDescription;
import aifolk.onto.vocab.ModelDescription;
import aifolk.onto.vocab.TaskDescription;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.util.logging.Logger;

@SuppressWarnings("javadoc")
public class AutoDriveFeature implements Feature {
	public AutoDriveFeature(MultiTreeMap configuration) {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public TaskDescription modelAlternate(String input, MLDriver mlDriver, OntologyDriver ontDriver,
			DataContext dataContext, Logger log) {
		// situation element: no of pedestrians
		ArrayList<Object> result = mlDriver.predict("YOLOv8-pedestrians", input, false);
		log.li("Result:", result);
		try {
			((DrivingDataContext) dataContext)
					.addPedestrianNumberData(Long.valueOf(Math.round(((Double) result.get(0)).doubleValue())));
		} catch(Exception e) {
			return null;
		}
		
		// match with current model description
		ModelDescription modelDesc = new ModelDescription(ontDriver.getGraph(),
				"http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#model_node_1");
		modelDesc.populateDescription(true);
		Integer pedestriansInModel = ((DrivingSceneContextDescription) modelDesc.getModelEvaluations().get(0)
				.getDatasetDescription().getDataContextDescriptions().get(0)).getAvgNumPedestrians().get();
		log.li("pedestrians in model:", pedestriansInModel);
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
		
		if(pedestriansInModel.intValue() < ((DrivingDataContext) dataContext).getMaxPedestrians().intValue()) {
			// time to change model
			log.li("must change model");
			final DrivingSceneContextDescription.Builder dataContextBuilder = DrivingSceneContextDescription.Builder
					.create("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#my_data_context_1");
			dataContextBuilder
					.withAvgNumPedestrians(((DrivingDataContext) dataContext).getMaxPedestrians().intValue());
			final DrivingSceneContextDescription dataContextDesc = dataContextBuilder.build();
			
			final TaskDescription.Builder taskBuilder = TaskDescription.Builder.create(
					"http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#my_task_1",
					"http://aimas.cs.pub.ro/ai-folk/ontology/core#SemanticSegmentation");
			taskBuilder.setDomainURI("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#AutonomousDriving");
			taskBuilder.addDataContextDescription(dataContextDesc);
			final TaskDescription taskDesc = taskBuilder.build();
			
			return taskDesc;
		}
		return null;
	}
}

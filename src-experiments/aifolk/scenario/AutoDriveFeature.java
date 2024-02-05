package aifolk.scenario;

import java.util.ArrayList;

import aifolk.core.DrivingDataContext;
import aifolk.core.MLManagementShard.Feature;
import aifolk.onto.vocab.DrivingSceneContextDescription;
import aifolk.onto.vocab.ModelDescription;
import aifolk.onto.vocab.TaskDescription;

public class AutoDriveFeature implements Feature {
	public TaskDescription modelAlternate() {
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
				.getDatasetDescription().getDataContextDescriptions().get(0)).getAvgNumPedestrians().get();
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
		
		if(pedestriansInModel.intValue() < ((DrivingDataContext) currentDataContext).getMaxPedestrians().intValue()) {
			// time to change model
			li("must change model");
			final DrivingSceneContextDescription.Builder dataContextBuilder = DrivingSceneContextDescription.Builder
					.create("http://aimas.cs.pub.ro/ai-folk/ontology/drivingSegmentation#my_data_context_1");
			dataContextBuilder
					.withAvgNumPedestrians(((DrivingDataContext) currentDataContext).getMaxPedestrians().intValue());
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

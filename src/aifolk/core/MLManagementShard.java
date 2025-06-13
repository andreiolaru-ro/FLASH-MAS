package aifolk.core;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

import aifolk.onto.OntologyDriver;
import aifolk.onto.vocab.ExportableDescription;
import aifolk.onto.vocab.TaskDescription;
import aifolk.scenario.ScenarioShard;
import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.flash.ml.MLPipelineShard;
import net.xqhs.util.logging.Logger;

public class MLManagementShard extends AgentShardGeneral {
	public static class DataContext {
		// No properties for now
	}

	public interface Feature {
		static final String FEATURE_KEY = "feature";

		TaskDescription modelAlternate(String modelName, String input, MLDriver mlDriver, OntologyDriver ontDriver,
				DataContext dataContext, Logger log);
	}

	private static final long serialVersionUID = 3728868349364945506L;
	private static final String DESIGNATION = "ML:Management";

	private MLDriver mlDriver;
	private OntologyDriver ontDriver;
	private DataContext currentDataContext = new DataContext();
	private Feature feature = null;

	public MLManagementShard() {
		super(AgentShardDesignation.customShard(DESIGNATION));
	}

	@Override
	public boolean configure(MultiTreeMap configuration) {
		for (String file : configuration.getValues(Feature.FEATURE_KEY)) {
			try {
				String cls = Loader.autoFind(PlatformUtils.getClassFactory(),
						configuration.getValues(CategoryName.PACKAGE.s()), file, file, null, Feature.FEATURE_KEY, null);
				feature = (Feature) PlatformUtils.getClassFactory().loadClassInstance(cls, null, false);
			} catch (ClassNotFoundException | InstantiationException | NoSuchMethodException | IllegalAccessException
					| InvocationTargetException e) {
				le("Feature load failed for feature [] with exception", file, e);
			}
		}
		return true;
	}

	@Override
	public boolean addGeneralContext(final EntityProxy<? extends Entity<?>> context) {
		if (!super.addGeneralContext(context))
			return false;
		if (context instanceof MLDriver) {
			mlDriver = (MLDriver) context;
			li("ML Driver detected");
			return true;
		}
		if (context instanceof OntologyDriver) {
			ontDriver = (OntologyDriver) context;
			li("Ontology Driver detected");
			return true;
		}
		return false;
	}

	@Override
	public void signalAgentEvent(final AgentEvent event) {
		super.signalAgentEvent(event);

		if (!(event instanceof AgentWave))
			return;

		AgentWave wave = (AgentWave) event;

		if (ScenarioShard.DESIGNATION.equals(wave.getValue(AgentWave.SOURCE_ELEMENT))) {
			// Received input for ML
			TaskDescription taskDesc = feature.modelAlternate("CombinedModel", (String) wave.getObject("input"),
					mlDriver, ontDriver, currentDataContext, this.getLogger());

			if (taskDesc != null) {
				// Replace these with actual logic if fields exist in TaskDescription
				String modelName = "CombinedModel";
				String taskType = "classification";

				Optional<String> taskDescStr = ExportableDescription.graphToString(taskDesc.exportToGraph());
				if (taskDescStr.isPresent()) {
					for (String friend : new String[] { "B", "C" }) {
						AgentWave message = new AgentWave(taskDescStr.get());
						message.addSourceElements(DESIGNATION);
						message.add(AIFolkProtocol.FOLK_PROTOCOL, AIFolkProtocol.FOLK_SEARCH);
						message.resetDestination(friend, DESIGNATION);

						sendMessage(message);
					}
				}

				MLPipelineShard pipeline = (MLPipelineShard) getAgent()
						.getAgentShard(AgentShardDesignation.customShard(MLPipelineShard.DESIGNATION));
				pipeline.setTaskModel(taskType, modelName);
			}
		}
	}

}

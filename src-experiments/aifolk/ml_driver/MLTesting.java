package aifolk.ml_driver;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.UnitComponent;

public class MLTesting extends AgentShardGeneral {
	
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = -2575234766537000937L;
	/**
	 * The log.
	 */
	transient UnitComponent		locallog			= null;
	
	/**
	 * The node-local {@link MLDriver} instance.
	 */
	MLDriver driver;
	
	/**
	 * No-argument constructor
	 */
	public MLTesting() {
		super(AgentShardDesignation.customShard(Boot.FUNCTIONALITY));
	}
	
	@Override
	protected void parentChangeNotifier(ShardContainer oldParent) {
		super.parentChangeNotifier(oldParent);
		if(getAgent() != null) {
			locallog = new UnitComponent("ml-" + getAgent().getEntityName()).setLogLevel(Level.ALL)
					.setLoggerType(PlatformUtils.platformLogType());
			locallog.lf("testing started.");
		}
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		super.addGeneralContext(context);
		if(!(context instanceof MLDriver)) {
			return false;
		}
		driver = (MLDriver) context;
		locallog.li("ML Driver detected");
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			// TODO testing code here
			/**
			 * An easy way to make tests here once we run this once already is to
			 * delete the newly added model form the /models directory, and restore the config file
			 * this will cause the model to be downloaded again, and the config file to be re-written
			 * otherwise, we can't download the model again, because it already exists, and we have to find new models
			 *
			 * Since the silero_stt model is too big to be uploaded to github,
			 * it has to be downloaded manually, you have to execute the following code in a python file:
			 *
			 * device = torch.device('cpu')  # gpu also works, but our models are fast enough for CPU
			 * model, decoder, utils = torch.hub.load(repo_or_dir='snakers4/silero-models',
			 *                                        model='silero_stt',
			 *                                        language='en', # also available 'de', 'es'
			 *                                        device=device)
			 * torch.jit.save(model, 'silero_stt.pth')
			 *
			 */

			li("Prediction result:", driver.predict("ResNet18", "src-experiments/aifolk/ml_driver/data/dog.jpg", true));

			Map<String, Object> modelConfig = new HashMap<>();
			modelConfig.put("cuda", true);
			modelConfig.put("transform", "datasets.transform.CityscapesTransform");
			modelConfig.put("task", "semantic segmentation");
			modelConfig.put("dataset", "Cityscapes");
			modelConfig.put("input_space", "RGB");
			driver.addModel("DeepLabV3Plus", "ml-directory/models/deeplabv3plus_cityscapes.pth", modelConfig);
			li("Models list:", driver.getModels().keySet().toString());
			driver.exportModel("ResNet18", "src-experiments/aifolk/ml_driver/test_export_destination");

			driver.addDataset("cityscapes", "[\"class1\", \"class2\"]");
			// loads some models, than [after some time] does several predictions, saves the model, etc
		default:
			break;
		}
	}
}

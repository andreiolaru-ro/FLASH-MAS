package aifolk.ml_driver;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.util.PlatformUtils;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.UnitComponent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
			 */

			driver.predict("MobileNetV2", "src/net/xqhs/flash/ml/python_module/data/dog.jpg");

			Map<String, Object> modelConfig = new HashMap<>();
			modelConfig.put("cuda", true);
			modelConfig.put("input_space", "RGB");
			modelConfig.put("input_size", List.of(224, 224));
			modelConfig.put("norm_std", List.of(0.229, 0.224, 0.225));
			modelConfig.put("norm_mean", List.of(0.485, 0.456, 0.406));
			List<String> classNames = List.of(
					"apple", "atm card", "cat", "banana", "bangle",
					"battery", "bottle", "broom", "bulb", "calender", "camera"
			);
			modelConfig.put("class_names", classNames);
			driver.addModel("src/net/xqhs/flash/ml/python_module/data/resnet18-bis.pth", modelConfig);
			driver.predict(("resnet18-bis"), "src/net/xqhs/flash/ml/python_module/data/dog.jpg");
			System.out.println(driver.getConfigFromYAML("resnet18-bis"));
			System.out.println(driver.getConfigFromServer("ResNet18"));
			System.out.println(driver.getModels());


			// loads some models, than [after some time] does several predictions, saves the model, etc
		default:
			break;
		}
	}
}

package aifolk.ml_driver;

import net.xqhs.flash.core.Entity;
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
	
	// when agent starts, it loads some models, than [after some time] does several predictions, saves the model, etc
	
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
}

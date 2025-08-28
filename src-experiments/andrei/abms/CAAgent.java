package andrei.abms;

import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.util.MultiTreeMap;

public class CAAgent extends BaseAgent {
	
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1453941340763410471L;
	protected static final String	STATE_PARAM			= "state";
	protected int					state				= 0;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		state = configuration.containsKey(STATE_PARAM) ? Integer.parseInt(configuration.getAValue(STATE_PARAM)) : 0;
		return true;
	}
}

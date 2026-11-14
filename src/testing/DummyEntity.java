package testing;

import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.support.Pylon;

/**
 * An example entity which is not pre-defined.
 * 
 * @author andreiolaru
 */
public class DummyEntity extends EntityCore<Pylon> {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean start() {
		li("Startup.");
		li("Context is: ", getFullContext());
		if(!super.start())
			return false;
		return stop();
	}
	
	@Override
	public boolean stop() {
		li("Shutdown");
		return super.stop();
	}
}

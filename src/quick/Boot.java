package quick;

import net.xqhs.flash.FlashBoot;

/**
 * This class is just a relay to {@link FlashBoot}.
 * 
 * @author Andrei Olaru
 */
public class Boot {
	
	/**
	 * Relays the call to {@link FlashBoot#main(String[])}.
	 * 
	 * @param args
	 *            - CLI arguments.
	 */
	public static void main(String[] args) {
		FlashBoot.main(args);
	}
	
}

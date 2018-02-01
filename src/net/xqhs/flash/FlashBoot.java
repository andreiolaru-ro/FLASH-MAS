package net.xqhs.flash;
import net.xqhs.flash.core.deployment.Boot;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.logging.Logging;

/**
 * Clas that boots a Flash-MAS instance.
 * 
 * @author andreiolaru
 */
public class FlashBoot
{
	/**
	 * Main method. It calls {@link Boot#boot(String[])} with the arguments received by the program.
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 */
	public static void main(String[] args)
	{
		Logging.getMasterLogging().setLogLevel(Level.ALL);
		new Boot().boot(args);
	}
	
}

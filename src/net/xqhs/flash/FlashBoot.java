package net.xqhs.flash;
import java.util.Arrays;

import net.xqhs.flash.core.node.NodeLoader;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.logging.LoggerSimple.Level;
import net.xqhs.util.logging.logging.Logging;

/**
 * Class that boots a Flash-MAS instance.
 * 
 * @author andreiolaru
 */
public class FlashBoot
{
	/**
	 * Main method. It calls {@link NodeLoader#load(TreeParameterSet)} with the arguments received by the program.
	 * 
	 * @param args
	 *            - the arguments received by the program.
	 */
	public static void main(String[] args)
	{
		Logging.getMasterLogging().setLogLevel(Level.ALL);
		String test_args;
		test_args = "";
//		test_args = "src-deployment/ChatAgents/deployment-chatAgents.xml";
//		test_args = "-support local host:here -agent bane something:something -component a";
//		test_args = "-support local -support local arg:val -support last host:here -agent bane something:something -feature a -feature b par:val -feature c -agent bruce -feature a";
		String[] use_args = test_args.split(" ");
		
		
		new NodeLoader().load(new TreeParameterSet().addAll("args", Arrays.asList(use_args)));
//		new Boot().boot(.split(" "));
	}
	
}

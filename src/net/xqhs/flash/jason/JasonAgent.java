package net.xqhs.flash.jason;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Example of an agent that only uses Jason BDI engine. It runs without all Jason IDE stuff. (see Jason FAQ for more
 * information about this example)
 *
 * The class must extend AgArch class to be used by the Jason engine.
 * 
 * @author andreiolaru, Jomi Hübner
 */
public class JasonAgent extends AgArch {
	
	protected static final String	LOAD_PARAM	= "load";
	private static Logger			logger		= Logger.getLogger(JasonAgent.class.getName());
	
	// public static void main(String[] a) {
	// new RunLocalMAS().setupLogger();
	// JasonAgent ag = new JasonAgent(null);
	// ag.run();
	// }
	
	public JasonAgent(MultiTreeMap configuration) {
		// set up the Jason agent
		logger.setLevel(Level.FINEST);
		String file = configuration.get(LOAD_PARAM);
		String loadFile = Loader.autoFind(Arrays.asList(new String[] { "jason" }), file, null,
				null, null, null);
		logger.fine("Loading " + loadFile);
		try {
			Agent ag = new Agent();
			new TransitionSystem(ag, null, null, this);
			ag.initAg();
			ag.load(loadFile);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Init error", e);
		}
	}
	
	public void run() {
		try {
			while(isRunning()) {
				// calls the Jason engine to perform one reasoning cycle
				logger.fine("Reasoning....");
				getTS().reasoningCycle();
				if(getTS().canSleep())
					sleep();
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Run error", e);
		}
	}
	
	public String getAgName() {
		return "bob";
	}
	
	// this method just add some perception for the agent
	@Override
	public List<Literal> perceive() {
		List<Literal> l = new ArrayList<Literal>();
		l.add(Literal.parseLiteral("x(10)"));
		return l;
	}
	
	// this method get the agent actions
	@Override
	public void act(ActionExec action) {
		getTS().getLogger().info("Agent " + getAgName() + " is doing: " + action.getActionTerm());
		// set that the execution was ok
		action.setResult(true);
		actionExecuted(action);
	}
	
	@Override
	public boolean canSleep() {
		return true;
	}
	
	@Override
	public boolean isRunning() {
		return true;
	}
	
	// a very simple implementation of sleep
	public void sleep() {
		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
		}
	}
	
	// Not used methods
	// This simple agent does not need messages/control/...
	@Override
	public void sendMsg(jason.asSemantics.Message m) throws Exception {
	}
	
	@Override
	public void broadcast(jason.asSemantics.Message m) throws Exception {
	}
	
	@Override
	public void checkMail() {
	}
	
}

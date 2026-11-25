package net.xqhs.flash.jason;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import jason.architecture.AgArch;
import jason.asSemantics.ActionExec;
import jason.asSemantics.Agent;
import jason.asSemantics.TransitionSystem;
import jason.asSyntax.Literal;
import jason.infra.local.RunLocalMAS;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.RunnableEntity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShardCore;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.support.PylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Example of an agent that only uses Jason BDI engine. It runs without all Jason IDE stuff. (see Jason FAQ for more
 * information about this example)
 *
 * The class must extend AgArch class to be used by the Jason engine.
 * 
 * @author andreiolaru, Jomi Hübner
 */
@SuppressWarnings("serial")
public class JasonAgent extends BaseAgent implements RunnableEntity<Pylon> {
	
	protected static final String				LOAD_PARAM	= "load";
	private static Logger						logger		= Logger.getLogger(JasonAgent.class.getName());
	protected AgArch							arch;
	protected Thread							thread;
	protected LinkedBlockingQueue<AgentEvent>	eventQueue	= null;
	MessagingShard								msg;
	BaseAgentProxy								asContext;
	
	// public static void main(String[] a) {
	// new RunLocalMAS().setupLogger();
	// JasonAgent ag = new JasonAgent(null);
	// ag.run();
	// }
	
	public class ArchImplem extends AgArch {
		@Override
		public String getAgName() {
			return getName();
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
			li("Agent " + getAgName() + " is doing: " + action.getActionTerm());
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
	
	@SuppressWarnings("unused")
	public JasonAgent(MultiTreeMap configuration) {
		// set up the Jason agent
		super();
		super.configure(configuration);
		
		asContext = new BaseAgentProxy() {
			@Override
			public String getEntityName() {
				return getName();
			}
			
			@Override
			public boolean postAgentEvent(AgentEvent event) {
				// TODO Auto-generated method stub
				eventQueue.add(event);
				return true;
			}
		};
		
		new RunLocalMAS().setupLogger();
		logger.setLevel(Level.FINEST);
		String file = configuration.get(LOAD_PARAM);
		String loadFile = Loader.autoFind(Arrays.asList(new String[] { "jason" }), file, null, null, null, null);
		li("Loading " + loadFile);
		try {
			arch = new ArchImplem();
			Agent ag = new Agent();
			new TransitionSystem(ag, null, null, arch);
			ag.initAg();
			ag.load(loadFile);
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Init error", e);
		}
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		msg = (MessagingShard) AgentShardCore.instantiateRecommendedShard(StandardAgentShard.MESSAGING,
				(PylonProxy) context, null, asContext);
		return super.addContext(context);
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		li("Messaging shard:", msg);
		thread = new Thread(this);
		thread.start();
		return true;
	}
	
	@Override
	public void run() {
		try {
			while(isRunning()) {
				// calls the Jason engine to perform one reasoning cycle
				// logger.fine("Reasoning....");
				arch.getTS().reasoningCycle();
				if(arch.getTS().canSleep())
					sleep();
			}
		} catch(Exception e) {
			logger.log(Level.SEVERE, "Run error", e);
		}
	}
	
	// a very simple implementation of sleep
	@SuppressWarnings("static-method")
	public void sleep() {
		try {
			Thread.sleep(200);
		} catch(InterruptedException e) {
		}
	}
	
}

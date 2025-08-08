package andrei.abms;

import java.util.LinkedList;
import java.util.List;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.deployment.Deployment;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Manages a group of agents.
 * 
 * @author Andrei Olaru
 */
public class AgentGroup extends EntityCore<Node> {
	/**
	 * 
	 */
	public static class AgentGroupLoader implements Loader<AgentGroup> {
		/**
		 * The load pack that will be used to load the agents.
		 */
		protected LoadPack lp;
		
		@Override
		public boolean configure(MultiTreeMap configuration, LoadPack loadPack) {
			lp = loadPack;
			return true;
		}
		
		@Override
		public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
			// TODO check that the base agent can be loaded
			return true;
		}
		
		@Override
		public boolean preload(MultiTreeMap configuration) {
			return preload(configuration, null);
		}
		
		@Override
		public AgentGroup load(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context,
				List<MultiTreeMap> subordinateEntities) {
			int n = Integer.parseInt(configuration.get("n"));
			int padLength = Integer.valueOf(n - 1).toString().length();
			List<String> subCateg = configuration.getTreeKeys();
			String c = subCateg.get(0);
			lp.lf("Loading n: [] entities of category: ", Integer.valueOf(n), c);
			// we support only one subcateg
			// do we support multiple trees per category?
			if(!c.equals(CategoryName.AGENT.getName()))
				return (AgentGroup) lp.lr(null, "AgentGroupLoader only supports agents as subordinate entity");
			MultiTreeMap subConfig = configuration.getATree(c)
					.getFirstTree(configuration.getATree(c).getTreeKeys().get(0));
			String baseName = subConfig.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			subConfig.clear(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			// lp.lf(subConfig.toString());
			
			List<MultiTreeMap> confs = new LinkedList<>();
			for(int i = 0; i < n; i++) {
				String agentName = baseName + DeploymentConfiguration.NAME_SEPARATOR + baseName
						+ String.format("%0" + padLength + "d", Integer.valueOf(i));
				confs.add(subConfig.copyShallow().addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, agentName));
				lp.lf("loading agent: ", agentName);
			}
			// context.add(this); // make an agent group proxy
			List<Entity<?>> entities = Deployment.get().loadEntities(confs, lp, context);
			List<Agent> agents = entities.stream().map(e -> (Agent) e).toList();
			AgentGroup ag = new AgentGroup(agents);
			ag.configure(configuration);
			return ag;
		}
		
		@Override
		public AgentGroup load(MultiTreeMap configuration) {
			return load(configuration, null, null);
		}
	}
	
	/**
	 * The serial UID
	 */
	private static final long	serialVersionUID	= 1L;
	/**
	 * The agents that are part of this group.
	 */
	protected List<Agent>		agents;
	
	/**
	 * Creates a new agent group with the given agents.
	 * 
	 * @param agentList
	 *            - the list of agents that are part of this group.
	 */
	public AgentGroup(List<Agent> agentList) {
		agents = agentList;
	}
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		int nStarted = 0;
		for(Agent agent : agents) {
			if(agent.start())
				nStarted++;
			else
				le("Agent [] could not be started.", agent.getName());
		}
		li("Started [] agents out of [].", Integer.valueOf(nStarted), Integer.valueOf(agents.size()));
		return nStarted == agents.size();
	}
	
	@Override
	public boolean stop() {
		if(!super.stop())
			return false;
		int nStopped = 0;
		for(Agent agent : agents) {
			if(agent.stop())
				nStopped++;
			else
				le("Agent [] could not be stopped.", agent.getName());
		}
		li("Stopped [] agents out of [].", Integer.valueOf(nStopped), Integer.valueOf(agents.size()));
		return nStopped == agents.size();
	}
}

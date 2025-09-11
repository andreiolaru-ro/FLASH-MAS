package andrei.abms;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import andrei.abms.gridworld.GridMap;
import andrei.abms.gridworld.GridPosition;
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
			int d = Integer.parseInt(configuration.get("d"));
			int padLength = Integer.valueOf(d - 1).toString().length();
			List<String> subCateg = configuration.getTreeKeys();
			String cat = subCateg.get(0);
			lp.lf("Loading n: [] entities of category: ", Integer.valueOf(d), cat);
			// we support only one subcateg
			// do we support multiple trees per category?
			if(!cat.equals(CategoryName.AGENT.getName()))
				return (AgentGroup) lp.lr(null, "AgentGroupLoader only supports agents as subordinate entity");
			MultiTreeMap subConfig = configuration.getATree(cat)
					.getFirstTree(configuration.getATree(cat).getTreeKeys().get(0));
			String baseName = subConfig.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			subConfig.clear(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			// lp.lf(subConfig.toString());
			
			java.util.Map<GridPosition, MultiTreeMap> confs = new LinkedHashMap<>();
			for(int y = 0; y < d; y++)
				for(int x = 0; x < d; x++) {
					String agentName = baseName + DeploymentConfiguration.NAME_SEPARATOR + baseName
							+ String.format("%0" + padLength + "d", Integer.valueOf(x))
							+ String.format("%0" + padLength + "d", Integer.valueOf(y));
					String state = ((y == 0 && x == 1) || (y == 1 && x == 2) || (y == 2 && x <= 2)) ? "1" : "0";
					confs.put(new GridPosition(x, y),
							subConfig.copyShallow().addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, agentName)
									.addOneValue(CAAgent.STATE_PARAM, state));
					lp.lf("loading agent [] with state ", agentName, state);
					
				}
			// context.add(this); // make an agent group proxy
			List<Entity<?>> entities = Deployment.get().loadEntities(new LinkedList<>(confs.values()), lp, context);
			List<Agent> agents = entities.stream().map(e -> (Agent) e).collect(Collectors.toList());
			// assume same order
			java.util.Map<GridPosition, Agent> agentMap = new LinkedHashMap<>();
			for(int i = 0; i < agents.size(); i++)
				agentMap.put(new ArrayList<>(confs.keySet()).get(i), agents.get(i));
			AgentGroup ag = new AgentGroup(agents);
			ag.configure(configuration);
			
			GridMap map = null;
			for(EntityProxy<? extends Entity<?>> c : context) {
				if(c instanceof GridMap) {
					map = (GridMap) c;
					agentMap.entrySet().stream().forEach(e -> ((GridMap) c).place(e.getValue(), e.getKey()));
				}
				if(c instanceof Executor) {
					((Executor) c).register(ag);
					agents.forEach(a -> ((Executor) c).register(a));
				}
			}
			ag.map = map;
			ag.d = d;
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
	protected int				d;
	protected GridMap			map;
	
	/**
	 * Creates a new agent group with the given agents.
	 * 
	 * @param agentList
	 *            - the list of agents that are part of this group.
	 */
	public AgentGroup(List<Agent> agentList) {
		agents = agentList;
	}
	
	protected void display() {
		String ret = "\n";
		for(int y = 0; y < d; y++) {
			for(int x = 0; x < d; x++) {
				CAAgent a = (CAAgent) map.get(new GridPosition(x, y));
				ret += a.state > 0 ? "X" : " ";
			}
			ret += "\n";
		}
		lf(ret);
	}
	
}

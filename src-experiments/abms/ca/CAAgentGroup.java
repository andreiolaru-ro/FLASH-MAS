package abms.ca;

import java.util.List;
import java.util.Map;

import net.xqhs.flash.abms.EntityGroup;
import net.xqhs.flash.abms.space.SpaceContext;
import net.xqhs.flash.abms.space.gridworld.GridPosition;
import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.util.MultiTreeMap;

public class CAAgentGroup extends EntityGroup {
	public class CAAgentGroupLoader extends EntityGroupLoader {
		Map<String, GridPosition> positions = null;
		
		@Override
		public boolean preload(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context) {
			int d = Integer.parseInt(configuration.get("d"));
			if(!super.preload(configuration.addOneValue(ENTITY_NUMBER_PARAM_NAME, Long.valueOf(d * d).toString()),
					context))
				return false;
			int padLength = Integer.valueOf(d - 1).toString().length();
			for(int y = 0; y < d; y++)
				for(int x = 0; x < d; x++) {
					String agentName = baseEntityName + DeploymentConfiguration.NAME_SEPARATOR + baseEntityName
							+ String.format("%0" + padLength + "d", Integer.valueOf(x))
							+ String.format("%0" + padLength + "d", Integer.valueOf(y));
					String state = ((y == 0 && x == 1) || (y == 1 && x == 2) || (y == 2 && x <= 2)) ? "1" : "0";
					entityConfigurations.get(y * d + x)
							.addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, agentName)
							.addOneValue(CAAgent.STATE_PARAM, state);
					positions.put(agentName, new GridPosition(x, y));
				}
			return true;
		}
		
		@Override
		public EntityGroup load(MultiTreeMap configuration, List<EntityProxy<? extends Entity<?>>> context,
				List<MultiTreeMap> subordinateEntities) {
			EntityGroup ag = super.load(configuration);
			if(ag == null)
				return null;
			if(context != null) {
				SpaceContext space = (SpaceContext) Loader.getClosestContext(context, SpaceContext.class);
				entities.forEach(e -> space.place(e.asContext(), positions.get(e.getName())));
			}
			// lp.lf("loading agent [] with state ", agentName, state);
			return ag;
		}
		
		@Override
		public EntityGroup load(MultiTreeMap configuration) {
			return load(configuration, null, null);
		}
		
	}
	
	public CAAgentGroup(List<Entity<?>> agentList) {
		super(agentList);
	}
	
}

package andrei.abms;

import java.util.List;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.deployment.LoadPack;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class AgentGroup extends EntityCore<Pylon> {
	/**
	 * 
	 */
	public static class AgentGroupLoader implements Loader<AgentGroup> {
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
			lp.lf("n: [] subs: ", n, subCateg);
			// we support only one subcateg
			String c = subCateg.get(0);
			// do we support multiple trees per category?
			MultiTreeMap subConfig = configuration.getATree(c)
					.getFirstTree(configuration.getATree(c).getTreeKeys().get(0));
			String baseName = subConfig.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
			lp.lf(subConfig.toString());
			
			for(int i = 0; i < n; i++) {
				String agentName = baseName + String.format("%0" + padLength + "d", i);
				MultiTreeMap conf = subConfig.copyShallow().addOneValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME,
						agentName);
				lp.lf("loading agent: ", agentName);
			}
			// Deployment.get().loadEntities(confs, node,
			
			return new AgentGroup();
		}
		
		@Override
		public AgentGroup load(MultiTreeMap configuration) {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	/**
	 * The serial UID
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		return true;
	}
	
}

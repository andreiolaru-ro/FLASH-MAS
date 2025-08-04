package andrei.abms;

import java.util.List;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.EntityCore;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class AgentGroup extends EntityCore<Pylon> implements Entity<Pylon> {
	/**
	 * The serial UID
	 */
	private static final long serialVersionUID = 1L;
	
	@Override
	public boolean start() {
		if(!super.start())
			return false;
		MultiTreeMap conf = getConfiguration();
		int n = Integer.parseInt(conf.get("n"));
		int padLength = Integer.valueOf(n - 1).toString().length();
		List<String> subCateg = conf.getTreeKeys();
		lf("n: [] subs: ", n, subCateg);
		// we support only one subcateg
		String c = subCateg.get(0);
		// do we support multiple trees per category?
		MultiTreeMap subConfig = conf.getATree(c).getFirstTree(conf.getATree(c).getTreeKeys().get(0));
		lf(subConfig.toString());
		
		for(int i = 0; i < n; i++) {
			String name = subConfig.get(DeploymentConfiguration.NAME_ATTRIBUTE_NAME)
					+ String.format("%0" + padLength + "d", i);
			lf(name);
		}
		
		return true;
	}
}

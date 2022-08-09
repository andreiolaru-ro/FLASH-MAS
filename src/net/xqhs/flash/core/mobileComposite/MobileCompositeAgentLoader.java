package net.xqhs.flash.core.mobileComposite;

import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.composite.CompositeAgentLoader;
import net.xqhs.flash.core.composite.CompositeAgentModel;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * An extension of {@link CompositeAgentLoader} that loads {@link MobileCompositeAgent} instances instead of
 * {@link CompositeAgent} instances.
 * 
 * @author Andrei Olaru
 */
public class MobileCompositeAgentLoader extends CompositeAgentLoader {
	@Override
	protected CompositeAgentModel createAgentInstance(MultiTreeMap agentConfiguration) {
		return new MobileCompositeAgent(agentConfiguration);
	}
}

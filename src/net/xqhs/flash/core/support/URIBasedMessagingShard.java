package net.xqhs.flash.core.support;

import java.net.URI;

import net.xqhs.flash.core.interoperability.InteroperableMessagingPylonProxy;
import net.xqhs.flash.core.shard.ShardContainer;

public class URIBasedMessagingShard extends AbstractMessagingShard {

	@Override
	protected void parentChangeNotifier(ShardContainer oldParent)
	{
		if(getAgent() != null && getAgent().getEntityName() != null) {
			try {
				URI uri = new URI(getAgent().getEntityName());
			} catch (Exception e) {
				throw new IllegalStateException("Address does not comply to the right URI format: " + getAgent().getEntityName());
			}
		}
		super.parentChangeNotifier(oldParent);
	}

	@Override
	public String getAgentAddress() {
		return getAgent().getEntityName();
	}

	@Override
	public String extractAgentAddress(String endpoint) {
		return endpoint.split(InteroperableMessagingPylonProxy.PLATFORM_PREFIX_SEPARATOR, 2)[0];
	}
}

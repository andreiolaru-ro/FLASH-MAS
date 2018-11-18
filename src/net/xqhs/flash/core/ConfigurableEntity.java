package net.xqhs.flash.core;

import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.config.Configurable;

public interface ConfigurableEntity<P extends Entity<?>> extends Entity<P>, Configurable
{
	public boolean configure(TreeParameterSet configuration);
}

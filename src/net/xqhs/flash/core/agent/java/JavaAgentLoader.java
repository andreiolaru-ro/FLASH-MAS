package net.xqhs.flash.core.agent.java;

import net.xqhs.flash.core.Loader;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.util.TreeParameterSet;
import net.xqhs.util.logging.Logger;

/**
 * {@link Loader} instance for agents implemented ad-hoc, in plain Java, potentially not using any of the features
 * offered by Flash-MAS.
 * 
 * @author andreiolaru
 */
public class JavaAgentLoader implements Loader<Agent>
{
	/**
	 * Logger to use, if set.
	 */
	Logger log = null;
	
	@Override
	public boolean configure(TreeParameterSet configuration, Logger _log)
	{
		log = _log;
		return true;
	}
	
	@Override
	public boolean preload(TreeParameterSet configuration)
	{
		// TODO Auto-generated method stub
		log.li("Config:", configuration);
		return false;
	}
	
	@Override
	public Agent load(TreeParameterSet configuration)
	{
		// TODO Auto-generated method stub
		return null;
	}
	
}

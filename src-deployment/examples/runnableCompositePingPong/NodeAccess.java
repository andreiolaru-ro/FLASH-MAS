package examples.runnableCompositePingPong;

import java.util.LinkedList;
import java.util.List;

import net.xqhs.flash.core.CategoryName;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;

// DO NOT USE THIS IN PRODUCTION
@SuppressWarnings("javadoc")
public class NodeAccess extends Node
{
	
	public NodeAccess(MultiTreeMap nodeConfiguration)
	{
		super(nodeConfiguration);
	}
	
	public List<Entity<?>> getAgents()
	{
		return registeredEntities.get(CategoryName.AGENT.getName());
	}
	
	public List<Entity<?>> getOtherEntities()
	{
		LinkedList<Entity<?>> ret = new LinkedList<>();
		for(String type : registeredEntities.keySet())
			ret.addAll(registeredEntities.get(type));
		return ret;
	}
}
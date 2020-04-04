package monitoringAndControl.monitoringAndControlTest;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.local.LocalSupport;


/**
 * The implementation of the agents.
 */
public class AgentTest implements Agent
{
	/**
	 * The messaging shard.
	 */
	MessagingShard					msgShard					= null;
	/**
	 * The name of this agent.
	 */
	String							agentName					= null;

	private static boolean RUNNING_STATE;

	private ShardContainer proxy = new ShardContainer() {

		@Override
		public String getEntityName()
		{
			return getName();
		}

		@Override
		public void postAgentEvent(AgentEvent event)
		{
			System.out.println(event.toString());
				if(event.getType().equals(AgentEventType.AGENT_WAVE))
				{
					String content = ((AgentWave) event).getContent();
					if (content.equals("stop")) {
						stop();
					}
				}
		}

		@Override
		public AgentShard getAgentShard(AgentShardDesignation designation)
		{
			return null;
		}
	};


	public AgentTest(MultiTreeMap configuration)
	{
		agentName = configuration.getFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME);
		msgShard = new LocalSupport.SimpleLocalMessaging();
		msgShard.addContext(proxy);
	}
	
	@Override
	public boolean start()
	{
		System.out.println(getName() + "Agent starting...");
		RUNNING_STATE = true;
		return true;
	}
	
	@Override
	public boolean stop()
	{
		System.out.println(getName() + "Agent stopped...");
		RUNNING_STATE = false;
		return false;
	}
	
	@Override
	public boolean isRunning()
	{
		return RUNNING_STATE;
	}
	
	@Override
	public String getName()
	{
		return agentName;
	}
	
	@Override
	public boolean addContext(EntityProxy<Pylon> context)
	{
		return msgShard.addGeneralContext(context);
	}
	
	@Override
	public boolean removeContext(EntityProxy<Pylon> context)
	{
		return false;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		return addContext((MessagingPylonProxy) context);
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		return false;
	}
	
	@Override
	public <C extends Entity<Pylon>> EntityProxy<C> asContext()
	{
		return null;
	}
	
}
package hpc_simulation.localTest;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.local.LocalPylon;
import net.xqhs.flash.local.LocalPylon.SimpleLocalMessaging;

@SuppressWarnings("javadoc")
class TestAgent implements Agent
{

	private String					name;
	private AbstractMessagingShard	messagingShard;
	private MessagingPylonProxy		pylon;
	public ShardContainer			proxy	= new ShardContainer() {
		@Override
												public boolean postAgentEvent(AgentEvent event)
		{
			if(event instanceof AgentWave)
				System.out.println(((AgentWave) event).getContent() + " de la "
						+ ((AgentWave) event).getCompleteSource() + " la "
						+ ((AgentWave) event).getCompleteDestination());
			int message = Integer.parseInt(((AgentWave) event).getContent());
			if(message < 5)
			{
				Thread eventThread = new Thread() {
					@Override
					public void run()
					{
						getMessagingShard()
								.sendMessage(
										getMessagingShard()
												.getAgentAddress(),
										((AgentWave) event)
												.getCompleteSource(),
										Integer.toString(
												message + 1));
					}
				};
				eventThread.run();
			}
													return true;
		}

		@Override
		public String getEntityName()
		{
			return getName();
		}

		@Override
		public AgentShard getAgentShard(AgentShardDesignation designation)
		{
			// not supported
			return null;
		}

	};

	public TestAgent(String name)
	{
		this.name = name;
	}

	@Override
	public boolean start()
	{
		if(name.equals("Two"))
		{
			messagingShard.sendMessage(messagingShard.getAgentAddress(), "One", "1");
		}
		return true;
	}

	@Override
	public boolean stop()
	{
		return true;
	}

	@Override
	public boolean isRunning()
	{
		return true;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public boolean addContext(EntityProxy<Pylon> context)
	{
		pylon = (MessagingPylonProxy) context;
		if(messagingShard != null)
			messagingShard.addGeneralContext(pylon);
		return true;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		return true;
	}

	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
	{
		return true;
	}

	@Override
	public boolean removeContext(EntityProxy<Pylon> context)
	{
		pylon = null;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Agent> asContext()
	{
		return proxy;
	}

	public boolean addMessagingShard(AbstractMessagingShard shard)
	{
		messagingShard = shard;
		shard.addContext(proxy);
		if(pylon != null)
			messagingShard.addGeneralContext(pylon);
		return true;
	}

	protected AbstractMessagingShard getMessagingShard()
	{
		return messagingShard;
	}
}

@SuppressWarnings("javadoc")
public class Main
{

	public static void main(String[] args)
	{
		LocalPylon pylon = new LocalPylon();

		TestAgent one = new TestAgent("One");
		one.addContext(pylon.asContext());
		TestAgent two = new TestAgent("Two");
		two.addContext(pylon.asContext());

		one.addMessagingShard(new SimpleLocalMessaging());
		two.addMessagingShard(new SimpleLocalMessaging());

		one.start();
		two.start();

	}

}
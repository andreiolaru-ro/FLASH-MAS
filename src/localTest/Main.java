package localTest;

import java.util.ArrayList;
import java.util.List;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContext;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.local.LocalSupport;
import net.xqhs.flash.local.LocalSupport.SimpleLocalMessaging;

class TestAgent implements Agent {

	private String name;
	private SimpleLocalMessaging messagingShard;
	private MessagingPylonProxy pylon;
	public ShardContext proxy = new ShardContext() {

		@Override
		public void postAgentEvent(AgentEvent event) {
			System.out.println(event.getValue(MessagingShard.CONTENT_PARAMETER) + " de la "
					+ event.getValue(MessagingShard.SOURCE_PARAMETER) + " la "
					+ event.getValue(MessagingShard.DESTINATION_PARAMETER));
			int message = Integer.parseInt(event.getValue(MessagingShard.CONTENT_PARAMETER));
			if (message < 5) {
				Thread eventThread = new Thread() {
					@Override
					public void run() {
						messagingShard.sendMessage(event.getValue(MessagingShard.DESTINATION_PARAMETER),
								event.getValue(MessagingShard.SOURCE_PARAMETER),
								Integer.toString(message + 1));
					}
				};
				eventThread.run();
			}
		}

		@Override
		public List<EntityProxy<Pylon>> getPylons() {
			ArrayList<EntityProxy<Pylon>> list = new ArrayList<EntityProxy<Pylon>>();
			list.add(pylon);
			return list;
		}

		@Override
		public String getAgentName() {
			return getName();
		}

	};

	public TestAgent(String name) {
		this.name = name;
	}

	@Override
	public boolean start() {
		if (name.equals("Two")) {
			messagingShard.sendMessage(this.getName(), "One", "1");
		}
		return true;
	}

	@Override
	public boolean stop() {
		return true;
	}

	@Override
	public boolean isRunning() {
		return true;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean addContext(EntityProxy<Pylon> context) {
		pylon = (MessagingPylonProxy) context;
		return true;
	}

	@Override
	public boolean addGeneralContext(EntityProxy<Entity<?>> context) {
		return true;
	}

	@Override
	public boolean removeContext(EntityProxy<Pylon> context) {
		pylon = null;
		return true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<Agent> asContext() {
		return proxy;
	}

	public boolean addMessagingShard(SimpleLocalMessaging shard) {
		messagingShard = shard;
		return true;
	}

}

public class Main {

	public static void main(String[] args) {
		LocalSupport pylon = new LocalSupport();

		TestAgent one = new TestAgent("One");
		one.addContext(pylon.asContext());
		TestAgent two = new TestAgent("Two");
		two.addContext(pylon.asContext());

		SimpleLocalMessaging shardOne = new SimpleLocalMessaging();
		SimpleLocalMessaging shardTwo = new SimpleLocalMessaging();

		shardOne.addContext(one.asContext());
		shardTwo.addContext(two.asContext());

		shardOne.register();
		shardTwo.register();

		one.addMessagingShard(shardOne);
		two.addMessagingShard(shardTwo);

		one.start();
		two.start();

	}

}

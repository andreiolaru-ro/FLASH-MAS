package localTest;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.local.LocalSupport;
import net.xqhs.flash.local.LocalSupport.SimpleLocalMessaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class AgentContainer {

	HashMap<Agent, List<Thread>> agentsActions;
	LinkedBlockingQueue<Thread> eventQueue;

	public void setEvent(Thread agentThread, Agent agent) {
		if (agentsActions.containsKey(agent)) {
			agentsActions.get(agent).add(agentThread);
		} else {
			agentsActions.put(agent, new ArrayList<Thread>());
			agentsActions.get(agent).add(agentThread);
		}
	}


	public void terminateEvent(Thread agentThread, Agent agent){
		if(agentsActions.isEmpty()) {
			return;
		} else {
			if (agentsActions.containsKey(agent)) {
				agentsActions.get(agent).remove(agentThread);
			}
		}
	}

	public void runAgents(){

	}

}


class TestNode extends Node
{
	public static final int MAX_THREADS = 3;
	public TestNode(String name)
	{
		super(name);
	}

	public void registerAgentsInNode( ArrayList<TestAgent> agentList)
	{
		for(int i = 0; i < agentList.size(); i++ )
		{
			TestAgent agent = agentList.get(i);
			registerEntity("Agent",agent, agent.getName() );
		}
	}

	/*Astea ar trebui mutate in Node. Metoda porneste toate entitatile inregistrate
	* intr-un nod. Pune task-urile agentilor intr-un thread pool.
	* Nu prea stiu cum ar functiona asta daca ei asteapta mesaje de la alti agenti
	* adormiti. POate doar daca ceva asteapta prea mult, sa intre in waiting.*/
	@Override
	public boolean start()
	{
		ExecutorService pool = Executors.newFixedThreadPool(MAX_THREADS);
		li("Starting node [].", name);
		for(Entity<?> entity : entityOrder)
		{
			if(!(entity instanceof Agent))
			{
				lf("starting an entity...");
				if (entity.start())
					lf("entity started successfully.");
				else
					le("failed to start entity.");
			} else {
				lf("starting an entity...");
				Runnable agentTask = () -> entity.start();

				pool.execute(agentTask);
			}
		}
		li("Node [] started.", name);
		pool.shutdown();

		return true;
	}
}

class TestAgent implements Agent
{
	/*Adaugate acum*/
	private boolean isAgentPaused;
	private ArrayList<TestAgent> messageTargetAgents;

	/*Originale*/
	private String					name;
	private AbstractMessagingShard	messagingShard;
	private MessagingPylonProxy		pylon;
	public ShardContainer			proxy	= new ShardContainer() {
												@Override
												public void postAgentEvent(AgentEvent event)
												{
													System.out.println(event.getValue(
															AbstractMessagingShard.CONTENT_PARAMETER) + " de la "
															+ event.getValue(AbstractMessagingShard.SOURCE_PARAMETER)
															+ " la " + event.getValue(
																	AbstractMessagingShard.DESTINATION_PARAMETER));
													int message = Integer.parseInt(
															event.getValue(AbstractMessagingShard.CONTENT_PARAMETER));
													if(message < 5)
													{
														Thread eventThread = new Thread() {
																									@Override
																									public void run()
																									{
																										getMessagingShard()
																												.sendMessage(
																														event.getValue(
																																AbstractMessagingShard.SOURCE_PARAMETER),
																														event.getValue(
																																AbstractMessagingShard.DESTINATION_PARAMETER),
																														Integer.toString(
																																message + 1));
																										//System.out.println(name + " " + this.getId() +" thread");
																									}
																								};
														eventThread.run();
														/*getMessagingShard()
																.sendMessage(
																		event.getValue(
																				AbstractMessagingShard.DESTINATION_PARAMETER),
																		event.getValue(
																				AbstractMessagingShard.SOURCE_PARAMETER),
																		Integer.toString(
																				message + 1));*/
													}
												}
												
												@Override
												public String getEntityName()
												{
													return getName();
												}
												
											};
	
	public TestAgent(String name)
	{
		this.name = name;
		isAgentPaused = false;
	}
	
	@Override
	public boolean start()
	{
		/*if(name.equals("Two"))
		{
			messagingShard.sendMessage( this.getName(), "One",  "1");

		}*/
		if(name.equals("2"))
		{
			broadcastToAgentList(this.messageTargetAgents);
		}
		return true;
	}
	
	@Override
	public boolean stop()
	{
		return true;
	}

	@Override
	public void run(){}

	public boolean pause()
	{
		isAgentPaused = true;
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

	public void broadcastToAgentList(ArrayList<TestAgent> agentList)
	{
		for(int i = 0; i < agentList.size(); i++)
		{
			if (i != Integer.parseInt(this.getName()))
			{
				//messagingShard.sendMessage(this.getName(), agentList.get(i).name, "1");
				messagingShard.sendMessage(agentList.get(i).getName(), this.getName(), "1");
			}

		}
	}

	public void setMessageTargetAgents(ArrayList<TestAgent> messageTargetAgents)
	{
		this.messageTargetAgents = messageTargetAgents;
	}
}

public class Main
{

	public static ArrayList<TestAgent> createAgentList(int agentCount)
	{
		ArrayList<TestAgent> agentList = new ArrayList<>();
		for(int i = 0; i < agentCount; i++)
		{
			agentList.add(new TestAgent(String.valueOf(i)));
		}

		return agentList;
	}

	public static void addContextToAgentList(LocalSupport pylon, ArrayList<TestAgent> agentList)
	{
		for(int i = 0; i < agentList.size(); i++)
		{
			agentList.get(i).addContext(pylon.asContext());
		}
	}

	public static void addMessagingShardToAgentList( ArrayList<TestAgent> agentList)
	{
		for(int i = 0; i < agentList.size(); i++)
		{
			agentList.get(i).addMessagingShard(new SimpleLocalMessaging());
		}
	}

	public static void startAgents(ArrayList<TestAgent> agentList)
	{
		for(int i = 0; i < agentList.size(); i++)
		{
			agentList.get(i).start();
		}
	}



	public static void addMessageTargetAgents(ArrayList<TestAgent> agentList)
	{
		for(int i = 0; i< agentList.size(); i++)
		{

		}
	}




	public static void main(String[] args)
	{
		/*LocalSupport pylon = new LocalSupport();
		Node node = new Node("testNode");
		
		TestAgent one = new TestAgent("One");
		one.addContext(pylon.asContext());
		TestAgent two = new TestAgent("Two");
		two.addContext(pylon.asContext());
		
		one.addMessagingShard(new SimpleLocalMessaging());
		two.addMessagingShard(new SimpleLocalMessaging());

		node.registerEntity("Agent", one, "One");
		node.registerEntity("Agent", two, "Two");

		//node.start();
		one.start();
		two.start();*/

		TestNode node = new TestNode("testNode");
		LocalSupport pylon = new LocalSupport();

		ArrayList<TestAgent> agentList;
		agentList = createAgentList(3);
		addContextToAgentList(pylon, agentList);
		addMessagingShardToAgentList(agentList);

		addMessageTargetAgents(agentList);
		agentList.get(2).setMessageTargetAgents(agentList);

		node.registerAgentsInNode(agentList);

		//startAgents(agentList);
		node.start();
		
	}
	
}

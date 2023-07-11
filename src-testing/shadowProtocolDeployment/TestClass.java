package shadowProtocolDeployment;

import java.io.FileReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.SimpleLoader;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import shadowProtocolDeployment.Action.Actions;
import testing.EchoTestingShard;
import wsRegions.WSRegionsShard;
import wsRegions.WSRegionsPylon;

/**
 * Class that generates and execute tests.
 */
public class TestClass {
	List<String>	regionServersList	= new ArrayList<>();
	List<String>	pylonsList			= new ArrayList<>();
	List<String>	agentsList			= new ArrayList<>();
	/**
	 * Current
	 */
	Topology		topology_map;
	/**
	 * Initial topology
	 */
	Topology		topology_init;
	/**
	 * Topology for <i>this</i> node.
	 */
	Topology		topology_for_node;
	static Integer	index_message		= 0;
	
	/**
	 * All constructed elements -- nodes, pylons, agents
	 */
	private final Map<String, Object> elements = new HashMap<>();
	
	/**
	 * Constructor
	 *
	 * @param filename
	 *            - the test file
	 */
	public TestClass(String filename) {
		try {
			Gson gson = new Gson();
			Reader reader = Files.newBufferedReader(Paths.get(filename));
			this.topology_map = gson.fromJson(Files.newBufferedReader(Paths.get(filename)), Topology.class);
			this.topology_init = gson.fromJson(Files.newBufferedReader(Paths.get(filename)), Topology.class);
			
			for(Map.Entry<String, Map<String, List<String>>> region : (this.topology_map.getTopology()).entrySet()) {
				this.regionServersList.add(region.getKey());
				for(Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
					this.pylonsList.add(pylon.getKey());
					this.agentsList.addAll(pylon.getValue());
				}
			}
			
			reader.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	public void addTopologyForNode(String filename) {
		try {
			Gson gson = new Gson();
			Reader reader = Files.newBufferedReader(Paths.get(filename));
			this.topology_for_node = gson.fromJson(Files.newBufferedReader(Paths.get(filename)), Topology.class);
			reader.close();
		} catch(Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * @param givenList
	 *            - the list used to extract an element
	 * @return - returns a random element from the given list
	 */
	public String getRandomElement(List<String> givenList) {
		Random rand = new Random();
		return givenList.get(rand.nextInt(givenList.size()));
	}
	
	/**
	 * Generate an action of type send_message between the selected agents.
	 *
	 * @return - returns an Action object.
	 */
	public Action sendMessageAction() {
		String source = getRandomElement(agentsList);
		List<String> copy = new ArrayList<>(agentsList);
		copy.remove(source);
		String destination = getRandomElement(copy);
		String complete_dest = destination + "-"
				+ topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, destination);
		String complete_source = source + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, source);
		return new Action(complete_source, complete_dest, "Message " + index_message++, Action.Actions.SEND_MESSAGE);
	}
	
	public Action sendMessageActionForAgent(String source) {
		List<String> copy = new ArrayList<>(agentsList);
		copy.remove(source);
		String destination = getRandomElement(copy);
		String complete_dest = destination + "-"
				+ topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, destination);
		String complete_source = source + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, source);
		return new Action(complete_source, complete_dest, "Message " + index_message++, Action.Actions.SEND_MESSAGE);
	}
	
	/**
	 * Generate an action of type move_to_another_node for a selected agent.
	 *
	 * @return - returns an Action object.
	 */
	public Action moveToAnotherNodeAction() {
		String agent = getRandomElement(agentsList);
		String agent_complete = agent + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, agent);
		List<String> copy = new ArrayList<>(pylonsList);
		// System.out.println(agent);
		copy.remove(topology_map.getter(Topology.GetterType.GET_PYLON_FOR_AGENT, agent));
		String pylon = getRandomElement(copy);
		String pylon_complete = pylon + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_PYLON, pylon);
		return new Action(agent_complete, pylon_complete, "", Action.Actions.MOVE_TO_ANOTHER_NODE);
	}
	
	public Action moveToAnotherNodeActionForAgent(String agent) {
		String agent_complete = agent + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, agent);
		List<String> copy = new ArrayList<>(pylonsList);
		// System.out.println(agent);
		copy.remove(topology_map.getter(Topology.GetterType.GET_PYLON_FOR_AGENT, agent));
		String pylon = getRandomElement(copy);
		String pylon_complete = pylon + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_PYLON, pylon);
		return new Action(agent_complete, pylon_complete, "", Action.Actions.MOVE_TO_ANOTHER_NODE);
	}
	
	/**
	 * @param numberOfMessages
	 *            - the number of send_message actions
	 * @param numberOfMoves
	 *            - the number of move actions
	 * @return - returns a list of actions
	 */
	public List<Action> generateTest(Integer numberOfMessages, Integer numberOfMoves) {
		List<Action> test = new ArrayList<>();
		List<Action.Actions> mess = new ArrayList<>(Arrays.asList(new Action.Actions[numberOfMessages]));
		List<Action.Actions> moves = new ArrayList<>(Arrays.asList(new Action.Actions[numberOfMoves]));
		List<Action.Actions> all_actions = new ArrayList<>();
		
		Collections.fill(mess, Action.Actions.SEND_MESSAGE);
		Collections.fill(moves, Action.Actions.MOVE_TO_ANOTHER_NODE);
		all_actions.addAll(mess);
		all_actions.addAll(moves);
		Collections.shuffle(all_actions);
		for(Action.Actions ac : all_actions) {
			switch(ac) {
			case MOVE_TO_ANOTHER_NODE:
				Action moveAC = moveToAnotherNodeAction();
				test.add(moveAC);
				topology_map.topologyAfterMove(moveAC);
				break;
			case SEND_MESSAGE:
				test.add(sendMessageAction());
			}
		}
		return test;
	}
	
	public List<Action> generateActionsForAgent(Integer numberOfMessages, Integer numberOfMoves, String agent) {
		List<Action> test = new ArrayList<>();
		List<Action.Actions> mess = new ArrayList<>(Arrays.asList(new Action.Actions[numberOfMessages]));
		List<Action.Actions> moves = new ArrayList<>(Arrays.asList(new Action.Actions[numberOfMoves]));
		List<Action.Actions> all_actions = new ArrayList<>();
		
		Collections.fill(mess, Action.Actions.SEND_MESSAGE);
		Collections.fill(moves, Action.Actions.MOVE_TO_ANOTHER_NODE);
		// all_actions.addAll(moves);
		// all_actions.addAll(mess);
		// Collections.shuffle(all_actions);
		if(numberOfMessages == numberOfMoves) {
			for(int i = 0; i < numberOfMessages; i++) {
				all_actions.add(mess.get(i));
				all_actions.add(moves.get(i));
			}
		}
		if(numberOfMessages > numberOfMoves) {
			for(int i = 0; i < numberOfMoves; i++) {
				all_actions.add(mess.get(i));
				all_actions.add(moves.get(i));
			}
			all_actions.addAll(mess.subList(numberOfMoves, numberOfMessages));
		}
		
		if(numberOfMessages < numberOfMoves) {
			for(int i = 0; i < numberOfMessages; i++) {
				all_actions.add(mess.get(i));
				all_actions.add(moves.get(i));
			}
			all_actions.addAll(moves.subList(numberOfMessages, numberOfMoves));
		}
		// System.out.println(all_actions);
		for(Action.Actions ac : all_actions) {
			switch(ac) {
			case MOVE_TO_ANOTHER_NODE:
				Action moveAC = moveToAnotherNodeActionForAgent(agent);
				test.add(moveAC);
				topology_map.topologyAfterMove(moveAC);
				break;
			case SEND_MESSAGE:
				test.add(sendMessageActionForAgent(agent));
			}
		}
		return test;
	}
	
	public List<Action> getActionsFromFile(String filename, String agent_name) {
		List<Action> test = new ArrayList<>();
		
		JSONParser parser = new JSONParser();
		JSONArray actions = null;
		try {
			Object obj = parser.parse(new FileReader(filename));
			JSONObject jsonObject = (JSONObject) obj;
			actions = (JSONArray) jsonObject.get(agent_name);
		} catch(Exception e) {
			System.out.println("No actions in file " + filename);
		}
		if(actions != null) {
			Iterator iterator = actions.iterator();
			while(iterator.hasNext()) {
				Object act = JSONValue.parse(iterator.next().toString());
				if(act == null)
					break;
				JSONObject action = (JSONObject) act;
				if(action.get("type") == null) {
					test.add(new Action(null, null, null, Actions.NOP));
					continue;
				}
				Action.Actions ac_type = Action.Actions.valueOf((String) action.get("type"));
				String source = (String) action.get("source");
				String destination = (String) action.get("destination");
				String source_complete = source + "-"
						+ topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, source);
				String destination_complete = null;
				switch(ac_type) {
				case SEND_MESSAGE:
					destination_complete = destination + "-"
							+ topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, destination);
					break;
				case MOVE_TO_ANOTHER_NODE:
					destination_complete = destination + "-"
							+ topology_init.getter(Topology.GetterType.GET_SERVER_FOR_PYLON, destination);
					break;
				// default:
				// throw new IllegalStateException("Unexpected value: " + ac_type);
				}
				
				test.add(new Action(source_complete, destination_complete, (String) action.get("content"), ac_type));
			}
		}
		System.out.println("Total actions: " + test);
		return test;
	}
	
	/**
	 * @param testCase
	 *            - the list of actions
	 * @return - returns actions for each agent
	 */
	public Map<String, List<Action>> filterActionsBySources(List<Action> testCase) {
		return testCase.stream().collect(Collectors.groupingBy(s -> s.source));
	}
	
	/**
	 * Create and start the entities
	 */
	public void CreateElements(List<Action> testCase, String actionsFrom, Integer numberOfMessages,
			Integer numberOfMoves, boolean generateActions) {
		// Map<String, List<Action>> sortActions = filterActionsBySources(testCase);
		
		for(Map.Entry<String, Map<String, List<String>>> region : (this.topology_for_node.getTopology()).entrySet()) {
			// for (Map.Entry<String, Map<String, List<String>>> region : (this.topology_init.getTopology()).entrySet())
			// {
			boolean isRegionServer = true;
			for(Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
				String port_value = ((region.getKey()).split(":"))[1];
				String server_name = region.getKey();
				String pylon_name = pylon.getKey();
				String node_name = "node-" + pylon_name + "-" + server_name;
				
				// CREATE PYLON
				WSRegionsPylon pylon_elem = new WSRegionsPylon();
				MultiTreeMap config = new MultiTreeMap()
						.addSingleValue(WSRegionsPylon.HOME_SERVER_ADDRESS_NAME, "ws://" + server_name)
						.addSingleValue("pylon_name", pylon_name);
				if(isRegionServer) {
					config.addSingleValue(WSRegionsPylon.HOME_SERVER_PORT_NAME, port_value).addSingleValue("servers",
							String.join("|", regionServersList));
					isRegionServer = false;
				}
				pylon_elem.configure(config);
				
				pylon_elem.start();
				elements.put(pylon.getKey(), pylon_elem);
				
				// CREATE NODE
				Node node = new Node(
						new MultiTreeMap().addFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, node_name)
								.addFirstValue("region-server", "ws://" + server_name));
				node.addGeneralContext(pylon_elem.asContext());
				elements.put(node_name, node);
				int delay = 5000;
				// CREATE AGENTS
				for(String agent : (pylon.getValue())) {
					String agent_name = agent + "-" + region.getKey();
					MobileCompositeAgent agent_elem = new MobileCompositeAgent(
							new MultiTreeMap().addSingleValue("agent_name", agent_name)
									.addFirstValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, agent_name));
					
					// ADD CONTEXT
					agent_elem.addContext(pylon_elem.asContext());
					agent_elem.addGeneralContext(node.asContext());
					
					// ADD SHARDS
					WSRegionsShard mesgShard = new WSRegionsShard();
					mesgShard.configure(new MultiTreeMap().addSingleValue("connectTo", pylon_elem.HomeServerAddressName)
							.addSingleValue("agent_name", agent_elem.getName()).addSingleValue(
									SimpleLoader.CLASSPATH_KEY, "net.xqhs.flash.shadowProtocol.ShadowAgentShard"));
					agent_elem.addShard(mesgShard);
					
					agent_elem.addShard(new EchoTestingShard());
					
					TestingShard testingShard = new TestingShard();
					// List<String> actionsToString = new ArrayList<>(List.of(""));
					// if (sortActions.get(agent_elem.getName()) != null) {
					// actionsToString =
					// sortActions.get(agent_elem.getName()).stream().map(Action::toJsonString).collect(Collectors.toList());
					// }
					List<String> actionsToString = null;
					if(generateActions) {
						actionsToString = generateActionsForAgent(numberOfMessages, numberOfMoves, agent).stream()
								.map(Action::toJsonString).collect(Collectors.toList());
						// System.out.println(actionsToString);
					}
					else {
						actionsToString = getActionsFromFile(actionsFrom + pylon_name + ".json", agent).stream()
								.map(Action::toJsonString).collect(Collectors.toList());
						// System.out.println(actionsToString);
					}
					testingShard.configure(
							new MultiTreeMap().addSingleValue("Actions_List", String.join(";", actionsToString))
									.addSingleValue("delay", String.valueOf(delay)));
					agent_elem.addShard(testingShard);
					System.out.println("Read " + actionsToString.size() + " actions for " + pylon_name + "/" + agent);
					
					delay = delay + 1000;
					
					// agent_elem.addShard(new MobilityTestShard());
					
					elements.put(agent, agent_elem);
				}
			}
		}
		for(Map.Entry<String, Object> elem : elements.entrySet()) {
			if(elem.getValue() instanceof Node) {
				((Node) elem.getValue()).start();
			}
			if(elem.getValue() instanceof MobileCompositeAgent) {
				((MobileCompositeAgent) elem.getValue()).start();
			}
		}
	}
}
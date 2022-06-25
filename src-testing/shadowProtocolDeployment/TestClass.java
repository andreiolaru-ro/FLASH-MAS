package shadowProtocolDeployment;

import com.google.gson.Gson;
import maria.MobilityTestShard;
import net.xqhs.flash.core.*;
import net.xqhs.flash.core.node.Node;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.shadowProtocol.ShadowAgentShard;
import net.xqhs.flash.shadowProtocol.ShadowPylon;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Class that generates and execute tests.
 */
public class TestClass {
    List<String> regionServersList = new ArrayList<>();
    List<String> pylonsList = new ArrayList<>();
    List<String> agentsList = new ArrayList<>();
    Topology topology_map;
    Topology topology_init;
    static Integer index_message = 0;

    private final Map<String, Object> elements = new HashMap<>();

    /**
     * Constructor
     *
     * @param filename - the test file
     */
    public TestClass(String filename) {
        try {
            Gson gson = new Gson();
            Reader reader = Files.newBufferedReader(Paths.get(filename));
            this.topology_map = gson.fromJson(Files.newBufferedReader(Paths.get(filename)), Topology.class);
            this.topology_init = gson.fromJson(Files.newBufferedReader(Paths.get(filename)), Topology.class);

            for (Map.Entry<String, Map<String, List<String>>> region : (this.topology_map.getTopology()).entrySet()) {
                this.regionServersList.add(region.getKey());
                for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                    this.pylonsList.add(pylon.getKey());
                    this.agentsList.addAll(pylon.getValue());
                }
            }

            reader.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @param givenList - the list used to extract an element
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
        String complete_dest = destination + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, destination);
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
        System.out.println(agent);
        copy.remove(topology_map.getter(Topology.GetterType.GET_PYLON_FOR_AGENT, agent));
        String pylon = getRandomElement(copy);
        String pylon_complete = pylon + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_PYLON, pylon);;
        return new Action(agent_complete, pylon_complete, "", Action.Actions.MOVE_TO_ANOTHER_NODE);
    }

    /**
     * @param numberOfMessages - the number of send_message actions
     * @param numberOfMoves    - the number of move actions
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
        for (Action.Actions ac : all_actions) {
            switch (ac) {
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

    /**
     * @param testCase - the list of actions
     * @return - returns actions for each agent
     */
    public Map<String, List<Action>> filterActionsBySources(List<Action> testCase) {
        return testCase.stream().collect(Collectors.groupingBy(s -> s.source));
    }

    /**
     * Create and start the entities
     */
    public void CreateElements(List<Action> testCase) {
        Map<String, List<Action>> sortActions = filterActionsBySources(testCase);

        for (Map.Entry<String, Map<String, List<String>>> region : (this.topology_init.getTopology()).entrySet()) {
            for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                String port_value = ((region.getKey()).split(":"))[1];
                String server_name = region.getKey();
                String pylon_name = pylon.getKey();
                String node_name = "node-" + pylon_name + "-" + server_name;

                // CREATE PYLON
                ShadowPylon pylon_elem = new ShadowPylon();
                pylon_elem.configure(
                        new MultiTreeMap().addSingleValue(ShadowPylon.HOME_SERVER_ADDRESS_NAME, "ws://" + server_name)
                                .addSingleValue(ShadowPylon.HOME_SERVER_PORT_NAME, port_value)
                                .addSingleValue("servers", String.join("|", regionServersList))
                                .addSingleValue("pylon_name", pylon_name));
                pylon_elem.start();
                elements.put(pylon.getKey(), pylon_elem);

                // CREATE NODE
                Node node = new Node(new MultiTreeMap().addSingleValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, node_name)
                        .addSingleValue("region-server", "ws://" + server_name));
                node.addGeneralContext(pylon_elem.asContext());
                elements.put(node_name, node);

                // CREATE AGENTS
                for (String agent : (pylon.getValue())) {
                    String agent_name = agent + "-" + region.getKey();
                    CompositeAgentTest agent_elem = new CompositeAgentTest(new MultiTreeMap().addSingleValue("agent_name", agent_name));

                    // ADD CONTEXT
                    agent_elem.addContext(pylon_elem.asContext());
                    agent_elem.addGeneralContext(node.asContext());

                    // ADD SHARDS
                    ShadowAgentShard mesgShard = new ShadowAgentShard();
                    mesgShard.configure(new MultiTreeMap().addSingleValue("connectTo", pylon_elem.HomeServerAddressName)
                            .addSingleValue("agent_name", agent_elem.getName())
                            .addSingleValue(SimpleLoader.CLASSPATH_KEY, "net.xqhs.flash.shadowProtocol.ShadowAgentShard"));
                    agent_elem.addShard(mesgShard);

                    SendMessageShard testingShard = new SendMessageShard();
                    List<String> actionsToString = new ArrayList<>(List.of(""));
                    if (sortActions.get(agent_elem.getName()) != null) {
                        actionsToString = sortActions.get(agent_elem.getName()).stream().map(Action::toJsonString).collect(Collectors.toList());
                    }
                    testingShard.configure(new MultiTreeMap().addSingleValue("Actions_List", String.join(";", actionsToString)));
                    agent_elem.addShard(testingShard);

                    agent_elem.addShard(new MobilityTestShard());

                    elements.put(agent, agent_elem);
                }
            }
        }
        for (Map.Entry<String, Object> elem : elements.entrySet()) {
            if (elem.getValue() instanceof Node) {
                ((Node) elem.getValue()).start();
            }
            if (elem.getValue() instanceof CompositeAgentTest) {
                ((CompositeAgentTest) elem.getValue()).start();
            }
        }
    }
}
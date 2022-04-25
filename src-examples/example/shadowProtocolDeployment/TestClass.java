package example.shadowProtocolDeployment;

import com.google.gson.Gson;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.shadowProtocol.ShadowAgentShard;
import net.xqhs.flash.shadowProtocol.ShadowPylon;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Class used in parsing the Json file that contains the topology.
 */
class Topology {
    private Map<String, Map<String, List<String>>> topology;

    public enum GetterType {
        GET_PYLON_FOR_AGENT,
        GET_SERVER_FOR_AGENT,
        GET_SERVER_FOR_PYLON,
    }

    public Map<String, Map<String, List<String>>> getTopology() {
        return topology;
    }

    public void setTopology(Map<String, Map<String, List<String>>> topology) {
        this.topology = topology;
    }

    public String getter(GetterType type, String entity) {
        for (Map.Entry<String, Map<String, List<String>>> region : topology.entrySet()) {
            for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                switch (type) {
                    case GET_PYLON_FOR_AGENT:
                        if ((pylon.getValue()).contains(entity)) return pylon.getKey();
                        break;
                    case GET_SERVER_FOR_AGENT:
                        if ((pylon.getValue()).contains(entity)) return region.getKey();
                        break;
                    case GET_SERVER_FOR_PYLON:
                        if((pylon.getKey()).equals(entity)) return region.getKey();
                        break;
                }
            }
        }
        return null;
    }

    public void topologyAfterMove(Action moveAction) {
        String nextPylon = moveAction.getDestination();
        String prevPylon = getter(GetterType.GET_PYLON_FOR_AGENT, moveAction.source);
        String agent = moveAction.source;
        topology.get(getter(GetterType.GET_SERVER_FOR_PYLON, nextPylon)).get(nextPylon).add(agent);
        topology.get(getter(GetterType.GET_SERVER_FOR_PYLON, prevPylon)).get(prevPylon).remove(agent);
        System.out.println(topology);
    }
}

/**
 * Class that describe an action.
 */
class Action {
    String source, destination, content;
    TestClass.Actions type;

    public Action(String source, String destination, String content, TestClass.Actions type) {
        this.source = source;
        this.destination = destination;
        this.content = content;
        this.type = type;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public TestClass.Actions getType() {
        return type;
    }

    public void setType(TestClass.Actions type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Action{" +
                "source='" + source + '\'' +
                ", destination='" + destination + '\'' +
                ", content='" + content + '\'' +
                ", type=" + type +
                '}';
    }
}

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

    public enum Actions {
        /**
         * An agent sends a message to another agent.
         */
        SEND_MESSAGE,
        /**
         * An agent moves on another node.
         */
        MOVE_TO_ANOTHER_NODE
    }

    /**
     * Constructor
     * @param filename
     *              - the test file
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
     * @param givenList
     *          - the list used to extract an element
     * @return
     *          - returns a random element from the given list
     */
    public String getRandomElement(List<String> givenList) {
        Random rand = new Random();
        return givenList.get(rand.nextInt(givenList.size()));
    }

    /**
     * Generate an action of type send_message between the selected agents.
     * @return
     *      - returns an Action object.
     */
    public Action sendMessageAction() {
        String source = getRandomElement(agentsList);
        List<String> copy = new ArrayList<>(agentsList);
        copy.remove(source);
        String destination = getRandomElement(copy);
        return new Action(source, destination, "Message " + index_message++, Actions.SEND_MESSAGE);
    }

    /**
     * Generate an action of type move_to_another_node for a selected agent.
     * @return
     *      - returns an Action object.
     */
    public Action moveToAnotherNodeAction() {
        String agent = getRandomElement(agentsList);
        List<String> copy = new ArrayList<>(pylonsList);
        copy.remove(topology_map.getter(Topology.GetterType.GET_PYLON_FOR_AGENT, agent));
        return new Action(agent, getRandomElement(copy), "", Actions.MOVE_TO_ANOTHER_NODE);
    }

    /**
     * @param numberOfMessages
     *          - the number of send_message actions
     * @param numberOfMoves
     *          - the number of move actions
     * @return
     *          - returns a list of actions
     */
    public List<Action> generateTest(Integer numberOfMessages, Integer numberOfMoves) {
        List<Action> test = new ArrayList<>();
        List<Actions> mess = new ArrayList<>(Arrays.asList(new Actions[numberOfMessages]));
        List<Actions> moves = new ArrayList<>(Arrays.asList(new Actions[numberOfMoves]));
        List<Actions> all_actions = new ArrayList<>();

        Collections.fill(mess, Actions.SEND_MESSAGE);
        Collections.fill(moves, Actions.MOVE_TO_ANOTHER_NODE);
        all_actions.addAll(mess);
        all_actions.addAll(moves);
        Collections.shuffle(all_actions);
        for (Actions ac: all_actions) {
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

        System.out.println();
        System.out.println(topology_map.getTopology());
        System.out.println();
        System.out.println(topology_init.getTopology());

        for (Action a : test) {
            System.out.println(a.toString());
        }
        return test;
    }

    /**
     * Create and start the entities
     */
    public void CreateElements() {
        for (Map.Entry<String, Map<String, List<String>>> region : (this.topology_init.getTopology()).entrySet()) {
            for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                String port_value = ((region.getKey()).split(":"))[1];
                ShadowPylon pylon_elem = new ShadowPylon();
                pylon_elem.configure(
                        new MultiTreeMap().addSingleValue(ShadowPylon.HOME_SERVER_ADDRESS_NAME, "ws://" + region.getKey())
                                .addSingleValue(ShadowPylon.HOME_SERVER_PORT_NAME, port_value)
                                .addSingleValue("servers", regionServersList.toString())
                                .addSingleValue("pylon_name", pylon.getKey()));
                pylon_elem.start();
                elements.put(pylon.getKey(), pylon_elem);

                for (String agent : (pylon.getValue())) {
                    AgentTestBoot.AgentTest agent_elem = new AgentTestBoot.AgentTest(agent + "-" + region.getKey());
                    agent_elem.addContext(pylon_elem.asContext());
                    agent_elem.addMessagingShard(new ShadowAgentShard(pylon_elem.HomeServerAddressName, agent_elem.getName()));
                    elements.put(agent, agent_elem);
                }
            }
        }
        for (Map.Entry<String, Object> elem : elements.entrySet()) {
            if (elem.getValue() instanceof AgentTestBoot.AgentTest) {
                ((AgentTestBoot.AgentTest) elem.getValue()).start();
            }
        }
    }

    /**
     * Run all actions previously generated
     * @param test
     *      - list of actions
     */
    public void runTest(List<Action> test) throws InterruptedException {
        for (Action action : test) {
            switch (action.getType()) {
                case MOVE_TO_ANOTHER_NODE:
                    AgentTestBoot.AgentTest agent = (AgentTestBoot.AgentTest) elements.get(action.getSource());
                    ShadowPylon dest_pylon = (ShadowPylon) elements.get(action.getDestination());

                    agent.moveToAnotherNode();
                    Thread.sleep(1000);
                    agent.addContext(dest_pylon.asContext());
                    agent.addMessagingShard(new ShadowAgentShard(dest_pylon.HomeServerAddressName, agent.getName()));
                    Thread.sleep(1000);
                    agent.reconnect();
                    break;
                case SEND_MESSAGE:
                    AgentTestBoot.AgentTest source = (AgentTestBoot.AgentTest) elements.get(action.getSource());
                    String destination = action.getDestination() + "-" + topology_init.getter(Topology.GetterType.GET_SERVER_FOR_AGENT, action.getDestination());
                    source.sendMessage(destination, action.getContent());
                    break;
            }
        }
    }

    /**
     * Stop the entities.
     */
    public void closeConnections() {
        for (Map.Entry<String, Object> elem : elements.entrySet()) {
            if (elem.getValue() instanceof ShadowPylon ) {
                ((ShadowPylon) elem.getValue()).stop();
            }
        }
    }
}
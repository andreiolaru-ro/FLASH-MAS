package example.shadowProtocolDeployment;

import com.google.gson.Gson;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.shadowProtocol.AgentShard;
import net.xqhs.flash.shadowProtocol.ShadowPylon;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

class Topology {
    private Map<String, Map<String, List<String>>> topology;

    public Map<String, Map<String, List<String>>> getTopology() {
        return topology;
    }

    public void setTopology(Map<String, Map<String, List<String>>> topology) {
        this.topology = topology;
    }

    public String getPylon(String agent) {
        for (Map.Entry<String, Map<String, List<String>>> region : topology.entrySet()) {
            for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                if ((pylon.getValue()).contains(agent)) {
                    return pylon.getKey();
                }
            }
        }
        return null;
    }

    public String getServerForAgent(String agent) {
        for (Map.Entry<String, Map<String, List<String>>> region : topology.entrySet()) {
            for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                if ((pylon.getValue()).contains(agent)) {
                    return region.getKey();
                }
            }
        }
        return null;
    }

    public String getServerForPylon(String pylonName) {
        for (Map.Entry<String, Map<String, List<String>>> region : topology.entrySet()) {
            for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                if((pylon.getKey()).equals(pylonName)) {
                    return region.getKey();
                }
            }
        }
        return null;
    }

    public void topologyAfterMove(Action moveAction) {
       // System.out.println(topology);
        String nextPylon = moveAction.getDestination();
        String prevPylon = getPylon(moveAction.source);
        String agent = moveAction.source;
        topology.get(getServerForPylon(nextPylon)).get(nextPylon).add(agent);
        topology.get(getServerForPylon(prevPylon)).get(prevPylon).remove(agent);
        System.out.println(topology);
    }
}

/**
 * To types of actions:
 *      Send_Message: source, destination, content
 *      Move_To_Another_Node: agent, destination
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

public class TestClass {
    List<String> regionServersList = new ArrayList<>();
    List<String> pylonsList = new ArrayList<>();
    List<String> agentsList = new ArrayList<>();
    Topology topology_map;
    Topology topology_init;
    static Integer index_message = 0;

    private Map<String, Object> elements = new HashMap<>();

    public enum Actions {
        SEND_MESSAGE,
        MOVE_TO_ANOTHER_NODE
    }

    public TestClass(String filename) {
        try {
            // create Gson instance
            Gson gson = new Gson();

            // create a reader
            Reader reader = Files.newBufferedReader(Paths.get(filename));
            Reader reader1 = Files.newBufferedReader(Paths.get(filename));
            // convert JSON file to map
            this.topology_map = gson.fromJson(reader, Topology.class);
            this.topology_init = gson.fromJson(reader1, Topology.class);

            System.out.println(this.topology_init.getTopology());

            for (Map.Entry<String, Map<String, List<String>>> region : (this.topology_map.getTopology()).entrySet()) {
                this.regionServersList.add(region.getKey());
                for (Map.Entry<String, List<String>> pylon : (region.getValue()).entrySet()) {
                    this.pylonsList.add(pylon.getKey());
                    this.agentsList.addAll(pylon.getValue());
                }
            }

            // close reader
            reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        //System.out.println(regionServersList);
        //System.out.println(pylonsList);
        //System.out.println(agentsList);
    }

    public String getRandomElement(List<String> givenList) {
        Random rand = new Random();
        return givenList.get(rand.nextInt(givenList.size()));
    }

    public Action sendMessageAction() {
        String source = getRandomElement(agentsList);
        List<String> copy = new ArrayList<>(agentsList);
        copy.remove(source);
        String destination = getRandomElement(copy);
        return new Action(source, destination, "Message " + index_message++, Actions.SEND_MESSAGE);
    }

    public Action moveToAnotherNodeAction() {
        String agent = getRandomElement(agentsList);
        List<String> copy = new ArrayList<>(pylonsList);
        copy.remove(topology_map.getPylon(agent));
        return new Action(agent, getRandomElement(copy), "", Actions.MOVE_TO_ANOTHER_NODE);
    }

    public List<Action> generateTest(Integer numberOfMessages, Integer numberOfMoves) {
        List<Action> test = new ArrayList<>();
        for (int i = 0; i < numberOfMessages; i++) {
            test.add(sendMessageAction());
        }
        for (int i = 0; i < numberOfMoves; i++) {
            Action moveAC = moveToAnotherNodeAction();
            test.add(moveAC);
            topology_map.topologyAfterMove(moveAC);
        }

        for (Action a : test) {
            System.out.println(a.toString());
        }

        return test;
    }

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
                    agent_elem.addMessagingShard(new AgentShard(pylon_elem.HomeServerAddressName, agent_elem.getName()));
                    elements.put(agent, agent_elem);
                }
            }
        }
        for (Map.Entry<String, Object> elem : elements.entrySet()) {
            if (elem.getValue() instanceof AgentTestBoot.AgentTest) {
                ((AgentTestBoot.AgentTest) elem.getValue()).start();
            }
        }

        //System.out.println(elements);
    }

    public void runTest(List<Action> test) throws InterruptedException {
        for (Action action : test) {
            //System.out.println(action.toString());
            switch (action.getType()) {
                case MOVE_TO_ANOTHER_NODE:
                    AgentTestBoot.AgentTest agent = (AgentTestBoot.AgentTest) elements.get(action.getSource());
                    ShadowPylon dest_pylon = (ShadowPylon) elements.get(action.getDestination());

                    agent.moveToAnotherNode();
                    Thread.sleep(1000);
                    agent.addContext(dest_pylon.asContext());
                    agent.addMessagingShard(new AgentShard(dest_pylon.HomeServerAddressName, agent.getName()));
                    Thread.sleep(1000);
                    agent.reconnect();
                    break;
                case SEND_MESSAGE:
                    AgentTestBoot.AgentTest source = (AgentTestBoot.AgentTest) elements.get(action.getSource());
                    String destination = action.getDestination() + "-" + topology_init.getServerForAgent(action.getDestination());

                    source.sendMessage(destination, action.getContent());
                    break;
            }
        }
    }

    public void closeConnections() {
        for (Map.Entry<String, Object> elem : elements.entrySet()) {
            if (elem.getValue() instanceof ShadowPylon ) {
                ((ShadowPylon) elem.getValue()).stop();
            }
        }
    }
}
package shadowProtocolDeployment;
import java.util.List;


public class CompositeAgentTestBoot {

    public static void main(String[] args) throws InterruptedException
    {
        TestClass test = new TestClass("src-testing/shadowProtocolDeployment/ExampleTopologyFiles/topology1_2_servers_2_pylons_2_agents_local.json");
//        TestClass test = new TestClass("src-testing/shadowProtocolDeployment/ExampleTopologyFiles/topology4_4_servers_8_pylons_16_agents.json");
//        test.addTopologyForNode("src-testing/shadowProtocolDeployment/Demo/topology1_node2.json");

       // List<Action> testCase = test.generateTest(0, 8);
        Validate_Results validator = new Validate_Results();

        test.CreateElements(null, 0, 0, false);
//        validator.validate_results(test.pylonsList);
    }
}

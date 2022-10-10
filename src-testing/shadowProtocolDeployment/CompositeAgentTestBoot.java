package shadowProtocolDeployment;

public class CompositeAgentTestBoot {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws InterruptedException
    {
    	String demo = "src-testing/shadowProtocolDeployment/Demo/topology1_2_servers_2_pylons_2_agents_local.json";
		String local = "src-testing/shadowProtocolDeployment/ExampleTopologyFiles/topology1_2_servers_2_pylons_2_agents_local.json";
		String local_no_agents = "src-testing/shadowProtocolDeployment/ExampleTopologyFiles/topology1_2_servers_2_pylons_0_agents.json";
		String node2 = "src-testing/shadowProtocolDeployment/Demo/topology1_node2.json";
		
		String actions_withMove = "src-testing/shadowProtocolDeployment/ActionsFor_3agents/";
		String actions_noMove = "src-testing/shadowProtocolDeployment/ActionsFor_3agents_msg_only/";
		String actions_1Move = "src-testing/shadowProtocolDeployment/ActionsFor_3agents_1move/";
		String noActions = "";
		
		TestClass test = new TestClass(local);

		// the topology for the current node
		test.addTopologyForNode(local);

       // List<Action> testCase = test.generateTest(0, 8);
        Validate_Results validator = new Validate_Results();

		test.CreateElements(null, actions_noMove, 0, 0, false);
//        validator.validate_results(test.pylonsList);
    }
}

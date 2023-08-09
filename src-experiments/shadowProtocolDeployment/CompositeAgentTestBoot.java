package shadowProtocolDeployment;

public class CompositeAgentTestBoot {

	@SuppressWarnings("unused")
	public static void main(String[] args) throws InterruptedException
    {
		String demo = "src-experiments/shadowProtocolDeployment/Demo/topology1_2_servers_2_pylons_2_agents_local.json";
		String local = "src-experiments/shadowProtocolDeployment/ExampleTopologyFiles/topology1_2_servers_2_pylons_2_agents_local.json";
		String local_1agent = "src-experiments/shadowProtocolDeployment/ExampleTopologyFiles/topology1_2_servers_2_pylons_1_agent_local.json";
		String local_no_agents = "src-experiments/shadowProtocolDeployment/ExampleTopologyFiles/topology1_2_servers_2_pylons_0_agents.json";
		String node2 = "src-experiments/shadowProtocolDeployment/Demo/topology1_node2.json";
		
		String multregions = "src-experiments/shadowProtocolDeployment/ExampleTopologyFiles/topology_2regions_4nodes.json";
		
		String actions_withMove = "src-experiments/shadowProtocolDeployment/ActionsFor_3agents/";
		String actions_noMove = "src-experiments/shadowProtocolDeployment/ActionsFor_3agents_msg_only/";
		String actions_1Move = "src-experiments/shadowProtocolDeployment/ActionsFor_3agents_1move/";
		String noActions = "";
		
		String actions_MR = "src-experiments/shadowProtocolDeployment/ActionsFor_multipleregions/";
		
		TestClass test = new TestClass(multregions);

		// the topology for the current node
		test.addTopologyForNode(multregions);

       // List<Action> testCase = test.generateTest(0, 8);
        Validate_Results validator = new Validate_Results();

		test.CreateElements(null, actions_MR, 0, 0, false);
//        validator.validate_results(test.pylonsList);
    }
}

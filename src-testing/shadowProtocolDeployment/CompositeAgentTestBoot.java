package shadowProtocolDeployment;

import javax.xml.validation.Validator;
import java.util.List;


public class CompositeAgentTestBoot {

    public static void main(String[] args) throws InterruptedException
    {
        TestClass test = new TestClass("src-testing/shadowProtocolDeployment/ExampleTopologyFiles/topology1_2_servers_2_pylons_2_agents.json");
        List<Action> testCase = test.generateTest(5, 5);

//        test.CreateElements(testCase);

        Validate_Results validator = new Validate_Results();
        validator.validate_results(test.pylonsList);
    }
}

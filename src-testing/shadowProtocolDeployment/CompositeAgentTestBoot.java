package shadowProtocolDeployment;

import java.util.List;


public class CompositeAgentTestBoot {

    public static void main(String[] args) throws InterruptedException
    {
        TestClass test = new TestClass("src-testing/shadowProtocolDeployment/RandomTestCases/Test1.json");
        List<Action> testCase = test.generateTest(10, 4);
        System.out.println(testCase);
        test.CreateElements(testCase);
    }
}

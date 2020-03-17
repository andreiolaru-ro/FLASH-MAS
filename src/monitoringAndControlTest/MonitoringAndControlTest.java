package monitoringAndControlTest;

import monitoringAndControl.MainBoard;

public class MonitoringAndControlTest {
    /**
     * Performs test.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        String test_args = "";

        test_args += "old-deployment/composite/basicScenario.xml";

        test_args += " -package deploymentTest -loader agent:composite";
        test_args += " -agent composite:AgentA -shard messaging -shard PingTestComponent -shard MonitoringTest";
        test_args += " -agent composite:AgentB -shard messaging -shard PingBackTestComponent -shard MonitoringTestShard";


        String finalTest_args = test_args;
        javax.swing.SwingUtilities.invokeLater(() -> new MainBoard(finalTest_args).createAndShowGUI());
    }
}







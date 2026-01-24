package fedlearn;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.flash.ml.PythonHTTPInterface;
import net.xqhs.util.logging.UnitComponent;

@SuppressWarnings("javadoc")
public class MLDriverTest {

	static UnitComponent l = new UnitComponent("tester");

	public static void main(String[] args) throws InterruptedException {
        MLDriver mld = new MLDriver(new PythonHTTPInterface());
        mld.configure(new MultiTreeMap().addSingleValue(
                DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "MLDriver"));
        mld.start();

        String datasetResult = mld.addDataset("TestDataset", "[\"person\", \"car\", \"dog\"]");
        System.out.println("Add dataset result: " + datasetResult);

        String image = MLDriver.ML_DIRECTORY_PATH + "input/a31ee6f_ADE_val_00000798.jpg";
        AgentWave request = new AgentWave(image);
		request.add("endpoint", "predict");
		request.add("model", "YOLOv8-pedestrians");

        mld.processAsync(request, reply -> System.out.println("Got async reply: " + reply.getContent()));

        Thread.sleep(5000);
        mld.stop();
	}
}

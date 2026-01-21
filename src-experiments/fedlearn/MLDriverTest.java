package fedlearn;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.util.logging.UnitComponent;

@SuppressWarnings("javadoc")
public class MLDriverTest {
	
	static UnitComponent l = new UnitComponent("tester");
	
	public static void main(String[] args) throws InterruptedException {
//		MLDriver mld = new MLDriver();
//		mld.configure(new MultiTreeMap().addSingleValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "MLDriver"));
//		l.lf("Starting driver");
//		mld.start();
//
//		String image = MLDriver.ML_DIRECTORY_PATH + "input/a31ee6f_ADE_val_00000798.jpg";
//		mld.predict("YOLOv8-pedestrians", image, false);
//
//		try {
//			Thread.sleep(3000);
//		} catch(InterruptedException e) {
//			e.printStackTrace();
//		}
//		l.lf("Stopping driver");
//		mld.stop();

        MLDriver mld = new MLDriver();
        mld.configure(new MultiTreeMap().addSingleValue(
                DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "MLDriver"));
        mld.start();

        String image = MLDriver.ML_DIRECTORY_PATH + "input/a31ee6f_ADE_val_00000798.jpg";
        AgentWave request = new AgentWave(image);

        mld.receiveAsync(request, reply -> {
            System.out.println("Got reply: " + reply.getContent());
        });

        Thread.sleep(5000);
        System.out.println(mld.receive(request).getContent());
        mld.stop();
	}
}

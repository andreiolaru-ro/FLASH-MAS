package fedlearn;

import java.util.ArrayList;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.ml.MLDriver;
import net.xqhs.util.logging.UnitComponent;

@SuppressWarnings("javadoc")
public class MLDriverTest {

	static UnitComponent l = new UnitComponent("tester");

	public static void main(String[] args) {
		MLDriver mld = new MLDriver();
		mld.configure(new MultiTreeMap().addSingleValue(DeploymentConfiguration.NAME_ATTRIBUTE_NAME, "MLDriver"));

		l.lf("Starting driver");
		mld.start();

		String imagePath = "C:\\Users\\teote\\Desktop\\Image\\111.jpg";

		try {
			l.lf("Sending prediction request to CombinedModel with image: " + imagePath);

			ArrayList<Object> response = mld.predict("CombinedModel", imagePath, false);

			if (response != null && !response.isEmpty()) {
				l.lf(" Prediction successful. Response list size: " + response.size());

				for (int i = 0; i < response.size(); i++) {
					l.lf("Result [" + i + "]: " + response.get(i));
				}
			} else {
				l.lf(" Received empty or null response from prediction.");
			}

		} catch (Exception e) {
			l.le(" Error during prediction: " + e.getMessage());
			e.printStackTrace();
		}

		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		l.lf("Stopping driver");
		mld.stop();
	}
}

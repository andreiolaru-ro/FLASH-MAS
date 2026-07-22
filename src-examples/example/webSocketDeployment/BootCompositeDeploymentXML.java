package example.webSocketDeployment;

import net.xqhs.flash.FlashBoot;

/**
 * Deployment testing.
 */
public class BootCompositeDeploymentXML {
    /**
     * Boots example.
     *
     * @param args_
     *            - not used.
     */
    public static void main(String[] args_) {
        FlashBoot.main(new String[] { "src-examples/example/webSocketDeployment/deployment.xml" });
    }
}

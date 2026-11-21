package example.webSocketDeployment;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.core.DeploymentConfiguration.SourceFileDirectory;
import net.xqhs.flash.core.util.PlatformUtils;

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
        FlashBoot.main(new String[] { PlatformUtils.buildFilePath(SourceFileDirectory.SRC_EXAMPLES,
                       BootCompositeDeploymentXML.class, "deployment.xml") });
    }
}

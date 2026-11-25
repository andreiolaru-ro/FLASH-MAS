package test.deployment.name;
import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.core.DeploymentConfiguration.SourceFileDirectory;
import net.xqhs.flash.core.util.PlatformUtils;
public class BootXML {
    public static void main(String[] args) {
        String test_args = PlatformUtils.buildFilePath(SourceFileDirectory.SRC_TESTS_MANUAL,
                                                       BootXML.class, "test-deployment-unnamed.xml");
        test_args += " -package testing";
        FlashBoot.main(test_args.split(" "));
    }
}

package test.deployment.name;
import net.xqhs.flash.FlashBoot;
public class BootXML {
    public static void main(String[] args) {
        String test_args = "src-tests/test/deployment/name/test-deployment-unnamed.xml -package testing";
        FlashBoot.main(test_args.split(" "));
    }
}

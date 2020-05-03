package interfaceGeneratorTest;

import interfaceGenerator.PageBuilder;
import interfaceGenerator.types.PlatformType;
import net.xqhs.flash.FlashBoot;

import java.util.ArrayList;
import java.util.Arrays;

public class DeploymentPageTestAgentGUIPassiveInput {
    public static void main(String[] args) {
        if (args.length == 0) {
            // YAML configuration provided in command line args
            System.err.println("No args provided");
            return;
        }

        String test_args = "";

        test_args += " -package interfaceGenerator";
        test_args += " -agent AgentA classpath:interfaceGenerator.AgentGUIPassiveInput";
        test_args += " -config";

        var args_list = new ArrayList<>(Arrays.asList(test_args.split(" ")));

        StringBuilder configuration = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            configuration.append(args[i]);
            if (i != args.length - 1) {
                configuration.append(" ");
            }
        }

        args_list.add(configuration.toString());

        var platformType = PlatformType.valueOfLabel(args[0]);
        System.err.println(platformType);
        if (platformType == null) {
            System.err.println("Invalid platform type");
            return;
        }
        PageBuilder.getInstance().platformType = platformType;
        System.err.println(PageBuilder.getInstance().platformType);

        FlashBoot.main(args_list.toArray(new String[0]));
    }
}

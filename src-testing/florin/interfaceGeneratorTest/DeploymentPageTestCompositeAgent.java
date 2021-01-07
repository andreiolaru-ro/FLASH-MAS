package florin.interfaceGeneratorTest;

import net.xqhs.flash.FlashBoot;
import net.xqhs.flash.gui.structure.types.PlatformType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import florin.PageBuilder;

public class DeploymentPageTestCompositeAgent {
    public static void main(String[] args) {

        if (args.length == 0) {
            // YAML configuration provided in command line args
            System.err.println("No args provided");
            return;
        }

        String test_args = "";

        PlatformType platformType = PlatformType.valueOfLabel(args[0]);
        System.err.println(platformType);
        if (platformType == null) {
            System.err.println("Invalid platform type");
            return;
        }
        PageBuilder.getInstance().platformType = platformType;

        test_args += " -package interfaceGenerator -loader agent:composite";
        test_args += " -agent composite:AgentA";
        test_args += " -shard gui.GUIShard";
        test_args += " -config";

        List<String> args_list = new ArrayList<>(Arrays.asList(test_args.split(" ")));

        StringBuilder configuration = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            configuration.append(args[i]);
            if (i != args.length - 1) {
                configuration.append(" ");
            }
        }

        args_list.add(configuration.toString());

        FlashBoot.main(args_list.toArray(new String[0]));
    }
}

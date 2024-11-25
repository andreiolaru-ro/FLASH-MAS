package net.xqhs.flash.testViorel;

import andrei.partialCLI.PartialCLI_Test;

public class PartialCLIWrapp {
    public static void processArgs (String[] argset) {
        if (argset == null || argset.length == 0){
            throw new IllegalArgumentException("Argset cannot be null or empty");
        }
        PartialCLI_Test.main(argset);
    }
}

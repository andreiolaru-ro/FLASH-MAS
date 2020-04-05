package interfaceGeneratorTest;

import interfaceGenerator.PageBuilder;

public class BuildPageTest {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("No file specified.");
            return;
        }
        var file = PageBuilder.buildPageFile(args[0]);
        if (file != null) {
            PageBuilder.buildPage(file);
        }
    }
}

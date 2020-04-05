package interfaceGeneratorTest;

import interfaceGenerator.PageBuilder;

public class BuildPageTest {
    private final static String INLINE = "inline";
    private final static String FILE = "file";

    private final static String inlineExample = "{platformType: desktop, node: {id: root, children: " +
            "[{id: child1, type: button, children: []}, {id: child2, children: []}]}}";

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Incorrect number of parameters");
            return;
        }

        if (args[0].equals(INLINE)) {
            // example: inline <yaml-specification>
            System.out.println("Inline");

            // with inline yaml
            var configuration = PageBuilder.buildPageInline(args[1]);
            PageBuilder.buildPage(configuration);
        } else if (args[0].equals(FILE)) {
            // example: file <file-path>
            System.out.println("Specified file");

            // with specified file
            var file = PageBuilder.buildPageFile(args[1]);
            if (file != null) {
                PageBuilder.buildPage(file);
            }
        } else {
            System.err.println("Invalid parameters");
        }
    }
}

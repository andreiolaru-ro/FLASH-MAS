package interfaceGeneratorTest;

import interfaceGenerator.PageBuilder;

public class BuildPageTest {
    public final static String INLINE = "inline";
    public final static String FILE = "file";

    public final static String inlineExample = "{platformType: desktop, node: {id: root, children: " +
            "[{id: child1, type: button, children: []}, {id: child2, children: []}]}}";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Incorrect number of parameters");
            return;
        }

        if (args[0].equals(INLINE)) {
            // example: inline <yaml-specification>
            System.out.println("Inline");

            // with inline yaml
            var configuration = PageBuilder.getInstance().buildPageInline(inlineExample);
            PageBuilder.getInstance().buildPage(configuration);
        } else if (args[0].equals(FILE)) {
            // example: file <file-path>
            System.out.println("Specified file");

            // with specified file
            var file = PageBuilder.getInstance().buildPageFile(args[1]);
            if (file != null) {
                PageBuilder.getInstance().buildPage(file);
            }
        } else {
            System.err.println("Invalid parameters");
        }
    }
}

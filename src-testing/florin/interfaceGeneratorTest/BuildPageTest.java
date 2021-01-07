package florin.interfaceGeneratorTest;

import florin.PageBuilder;
import florin.Utils;
import net.xqhs.flash.gui.structure.Element;
import net.xqhs.flash.gui.structure.GlobalConfiguration;
import net.xqhs.flash.sclaim.constructs.ClaimAgentDefinition;
import net.xqhs.flash.sclaim.parser.Parser;

public class BuildPageTest {
    public final static String INLINE = "inline";
    public final static String FILE = "file";
    public final static String CLAIM = "claim";

    public final static String inlineExample = "{platformType: desktop, node: {id: root, children: " +
            "[{id: child1, type: button, children: []}, {id: child2, children: []}]}}";

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("Incorrect number of parameters");
            return;
        }

        switch (args[0]) {
            case INLINE:
                // example: inline <yaml-specification>
                System.out.println("Inline");

                // with inline yaml
                GlobalConfiguration configuration = PageBuilder.getInstance().buildPageInline(inlineExample);
                PageBuilder.getInstance().buildPage(configuration);
                break;
            case FILE:
                // example: file <file-path>
                System.out.println("Specified file");

                // with specified file
                GlobalConfiguration file = PageBuilder.getInstance().buildPageFile(args[1]);
                if (file != null) {
                    PageBuilder.getInstance().buildPage(file);
                }
                break;
            case CLAIM:
                ClaimAgentDefinition agent = Parser.parseFile(args[1]);
                Element element = Utils.convertClaimAgentDefinitionToElement(agent);
                PageBuilder.getInstance().buildPage(element);
                break;
            default:
                System.err.println("Invalid parameters");
                break;
        }
    }
}

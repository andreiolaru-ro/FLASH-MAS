package easyLog.src.main.java.configuration.configParser.entities.selectors.output.types;

import easyLog.src.main.java.configuration.configParser.entities.selectors.expect.Expect;
import easyLog.src.main.java.configuration.configParser.entities.selectors.level.Level;
import easyLog.src.main.java.configuration.configParser.entities.selectors.output.OutputElement;

public class ExpectOutput implements OutputElement {

    Expect expect;

    public ExpectOutput(Expect expectElement) {
        this.expect = expectElement;
    }

    @Override
    public void build() {
        if (expect.isSatisfied()) {
            System.out.print(expect.getSatisfactionIndication() + " ");
        }
    }

    @Override
    public void addMatch(String logLine, String entity, String match, Level level) {

    }
}

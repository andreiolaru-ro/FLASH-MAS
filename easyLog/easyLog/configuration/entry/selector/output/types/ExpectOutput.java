package easyLog.configuration.entry.selector.output.types;

import easyLog.configuration.entry.selector.expect.Expect;
import easyLog.configuration.entry.selector.level.Level;
import easyLog.configuration.entry.selector.output.OutputElement;

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
        else {
            if(expect.getSatisfactionIndication() == null)
            {
                System.out.print(0 + " ");
            }
            else {
                System.out.print(expect.getSatisfactionIndication() + " ");
            }
        }
    }

    @Override
    public void addMatch(String logLine, String entity, String match, Level level) {

    }
}

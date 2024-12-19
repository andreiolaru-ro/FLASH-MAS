package easyLog.configuration.entry.selector.output.types;

import easyLog.configuration.entry.selector.level.Level;
import easyLog.configuration.entry.selector.output.OutputElement;

public class StringOutput implements OutputElement {
    String text;
    public StringOutput(String text) {
        // TODO Auto-generated constructor stub
        this.text = text;
    }

    @Override
    public void build() {
        System.out.print(text + " ");
    }

    @Override
    public void addMatch(String logLine, String entity, String match, Level level) {

    }
}

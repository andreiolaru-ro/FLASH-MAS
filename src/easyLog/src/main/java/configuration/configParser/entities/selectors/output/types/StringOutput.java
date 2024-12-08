package easyLog.src.main.java.configuration.configParser.entities.selectors.output.types;

import easyLog.src.main.java.configuration.configParser.entities.selectors.level.Level;
import easyLog.src.main.java.configuration.configParser.entities.selectors.output.OutputElement;

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

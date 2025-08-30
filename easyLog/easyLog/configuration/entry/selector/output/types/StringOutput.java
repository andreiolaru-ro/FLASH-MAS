package easyLog.configuration.entry.selector.output.types;

import easyLog.configuration.entry.Entry.OutputBlockAccess;
import easyLog.configuration.entry.selector.level.Level;
import easyLog.configuration.entry.selector.output.OutputElement;

public class StringOutput implements OutputElement {
    String text;
    public StringOutput(String text) {
        // TODO Auto-generated constructor stub
        this.text = text;
    }

    @Override
	public void build(OutputBlockAccess oneLineOutput, OutputBlockAccess blockOutput) {
		oneLineOutput.addOutputElement(text);
		blockOutput.addOutputElement(text);
    }

    @Override
    public void addMatch(String logLine, String entity, String match, Level level) {

    }
}

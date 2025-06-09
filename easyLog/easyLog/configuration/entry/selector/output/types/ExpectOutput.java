package easyLog.configuration.entry.selector.output.types;

import easyLog.configuration.entry.Entry.OutputBlockAccess;
import easyLog.configuration.entry.selector.expect.Expect;
import easyLog.configuration.entry.selector.level.Level;
import easyLog.configuration.entry.selector.output.OutputElement;

public class ExpectOutput implements OutputElement {
	
	Expect expect;
	
	public ExpectOutput(Expect expectElement) {
		this.expect = expectElement;
	}
	
	@Override
	public void build(OutputBlockAccess oneLineOutput, OutputBlockAccess blockOutput) {
		if(expect.isSatisfied()) {
			oneLineOutput.addOutputElement(expect.getSatisfactionIndication());
			blockOutput.addOutputElement(expect.getSatisfactionIndication());
		}
		else {
			if(expect.getSatisfactionIndication() == null) {
				oneLineOutput.addOutputElement(Integer.toString(0));
				blockOutput.addOutputElement(Integer.toString(0));
			}
			else {
				oneLineOutput.addOutputElement(expect.getSatisfactionIndication());
				blockOutput.addOutputElement(expect.getSatisfactionIndication());
			}
		}
	}
	
	@Override
	public void addMatch(String logLine, String entity, String match, Level level) {
		
	}
}

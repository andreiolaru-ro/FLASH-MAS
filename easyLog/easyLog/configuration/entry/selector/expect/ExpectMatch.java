package easyLog.configuration.entry.selector.expect;

import easyLog.configuration.entry.Entry.OutputBlockAccess;
import easyLog.configuration.entry.selector.level.Level;

public class ExpectMatch implements Expect {


    @Override
	public void build(OutputBlockAccess oneLineOutput, OutputBlockAccess blockOutput) {
		// TODO Auto-generated method stub
		
    }

    @Override
    public void addMatch(String logLine, String entity, String match, Level level) {

    }

    @Override
    public boolean isSatisfied() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getSatisfactionIndication() {
        // TODO Auto-generated method stub
        return null;
    }
}

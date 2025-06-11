package easyLog.configuration.entry.selector.output.types;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import easyLog.configuration.entry.Entry.OutputBlock;
import easyLog.configuration.entry.Entry.OutputBlockAccess;
import easyLog.configuration.entry.selector.level.Level;
import easyLog.configuration.entry.selector.output.OutputElement;
import easyLog.configuration.entry.selector.output.OutputListType;

public class ListOutput implements OutputElement {
    OutputListType outputListType;

    List<String> lines = new ArrayList<>();
    Set<String> entities = new HashSet<>();
    List<String> match =new ArrayList<>();
    Level level;
    public ListOutput(OutputListType type) {
        // TODO Auto-generated constructor stub
        this.outputListType = type;
    }

    @Override
	public void build(OutputBlockAccess oneLineOutput, OutputBlockAccess blockOutput) {
		oneLineOutput.addOutputElement("[");
        switch(outputListType){
            case ENTITIES:
                for(String entity: entities)
                {
					oneLineOutput.addOutputElement(entity);
					blockOutput.addOutputElement(entity);
                }
                break;
            case LINE:
				if(!lines.isEmpty())
					blockOutput.addOutputElement(OutputBlock.LINE_SEPARATOR);
				for(String line : lines)
                {
					oneLineOutput.addOutputElement(".");
					blockOutput.addOutputElement(line + OutputBlock.LINE_SEPARATOR);
                }
                break;
        }
		oneLineOutput.addOutputElement("]");
    }

    @Override
    public void addMatch(String logLine, String entity, String match, Level level) {
       if(this.level == null)
       {
           this.level = level;
       }
       this.lines.add(logLine);
       this.entities.add(entity);
       this.match.add(match);
    }
}

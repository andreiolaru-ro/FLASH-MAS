package easyLog.src.main.java.configuration.configParser.entities.selectors.output.types;

import easyLog.src.main.java.configuration.configParser.entities.selectors.level.Level;
import easyLog.src.main.java.configuration.configParser.entities.selectors.output.OutputElement;
import easyLog.src.main.java.configuration.configParser.entities.selectors.output.OutputListType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public void build() {

        switch(outputListType){
            case ENTITIES:
                for(String entity: entities)
                {
                    System.out.print(entity + ", ");
                }
                break;
            case LINE:
                for(String line: lines)
                {
                    System.out.println();
                    System.out.println(line);
                }
                break;
        }
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

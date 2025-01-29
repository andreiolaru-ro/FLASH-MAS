package easyLog;

import java.util.List;

import easyLog.configuration.entry.Entry;
import easyLog.configuration.entry.selector.output.OutputElement;
import easyLog.configuration.entry.selector.output.types.ListOutput;

public class ParserEngine implements LineProcessor {


    Entry entry;


    String regex = "\\[(.*?)\\]";

    public ParserEngine(Entry entry) {
        this.entry = entry;
    }


    @Override
    public void process(String line) {
        if (line.matches(".*\\[ " + entry.getEntity() + " \\].*")) { // face match orice e [  ] ca regex
            if (entry.getLevel().getType() != null) {
                if (line.startsWith(entry.getLevel().getType())) {
                    if (entry.getStateMatcher().getKeywords() != null) {
                        verifyMatch(line);
                    }
                    else //count only the logs that contain the level
                    {
                        entry.getExpect().addMatch(line, entry.getEntity(), entry.getLevel().getType(), entry.getLevel());
                        List<OutputElement> elementList = entry.getOutputItem().getElements();
                        for (OutputElement obj : elementList) {
                            if (obj instanceof ListOutput) {
                                obj.addMatch(line, entry.getEntity(), entry.getLevel().getType(), entry.getLevel());
                            }
                        }
                    }
                }
            } else {
                if (entry.getStateMatcher().getKeywords() != null) {
                    verifyMatch(line);
                }
            }
        }

    }


    public void verifyMatch(String line) {
        for (String item : entry.getStateMatcher().getKeywords()) {
            if (line.contains(item)) {
                entry.getExpect().addMatch(line, entry.getEntity(), item, entry.getLevel());
                List<OutputElement> elementList = entry.getOutputItem().getElements();
                for (OutputElement obj : elementList) {
                    if (obj instanceof ListOutput) {
                        obj.addMatch(line, entry.getEntity(), item, entry.getLevel());
                    }
                }
            }
        }
    }
}

package easyLog.configuration.processor.types;

import easyLog.configuration.entry.Entry;
import easyLog.configuration.entry.selector.output.OutputElement;
import easyLog.configuration.entry.selector.output.types.ListOutput;
import easyLog.configuration.processor.LineProcessor;

import java.util.List;

public class All implements LineProcessor {
    Entry entry;

    public All(Entry entry)
    {
        this.entry = entry;
    }
    @Override
    public void process(String line) {
        if (entry.getLevel().getType() != null) {
            if (line.startsWith(entry.getLevel().getType())) {
                if (entry.getStateMatcher().getKeywords() != null) {
                    verifyMatch(line, "*");
                } else //count only the logs that contain the level
                {
                    entry.getExpect().addMatch(line, "*", entry.getLevel().getType(), entry.getLevel());
                    List<OutputElement> elementList = entry.getOutputItem().getElements();
                    for (OutputElement obj : elementList) {
                        if (obj instanceof ListOutput) {
                            obj.addMatch(line, "*", entry.getLevel().getType(), entry.getLevel());
                        }
                    }
                }

            }
        } else {
            if (entry.getStateMatcher().getKeywords() != null) {
                verifyMatch(line, "*");
            }
        }
    }

    public void verifyMatch(String line, String matchedContent) {
        for (String item : entry.getStateMatcher().getKeywords()) {
            if (line.contains(item)) {
                entry.getExpect().addMatch(line, matchedContent, item, entry.getLevel());
                List<OutputElement> elementList = entry.getOutputItem().getElements();
                for (OutputElement obj : elementList) {
                    if (obj instanceof ListOutput) {
                        obj.addMatch(line, matchedContent, item, entry.getLevel());
                    }
                }
            }
        }
    }
}

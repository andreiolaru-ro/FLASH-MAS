package easyLog.configuration.processor.types;

import easyLog.configuration.entry.Entry;
import easyLog.configuration.entry.selector.output.OutputElement;
import easyLog.configuration.entry.selector.output.types.ListOutput;
import easyLog.configuration.processor.LineProcessor;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Agents implements LineProcessor {
    String regex = "\\[(.*?)\\]"; //captures what is inside the brackets
    Entry entry;

    public Agents(Entry entry) {
        this.entry = entry;
    }

    @Override
    public void process(String line) {
        if (line.matches(".*\\[ Agent.*")) {
            Pattern pattern = Pattern.compile(this.regex);
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                String matchedContent = matcher.group(1).trim();
                if (entry.getEntity().toLowerCase().substring(0, entry.getEntity().length() - 1).equals(matchedContent.toLowerCase().substring(0, matchedContent.length() - 2))) {
                    if (entry.getLevel().getType() != null) {
                        if (line.startsWith(entry.getLevel().getType())) {
                            if (entry.getStateMatcher().getKeywords() != null) {
                                verifyMatch(line, matchedContent);
                            } else //count only the logs that contain the level
                            {
                                entry.getExpect().addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
                                List<OutputElement> elementList = entry.getOutputItem().getElements();
                                for (OutputElement obj : elementList) {
                                    if (obj instanceof ListOutput) {
                                        obj.addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
                                    }
                                }
                            }
                        }
                    } else {
                        if (entry.getStateMatcher().getKeywords() != null) {
                            verifyMatch(line, matchedContent);
                        }
                    }
                }
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
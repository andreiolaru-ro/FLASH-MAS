package easyLog.src.main.java.parser.engine;

import easyLog.src.main.java.configuration.configParser.entities.Entry;
import easyLog.src.main.java.configuration.configParser.entities.selectors.output.OutputElement;
import easyLog.src.main.java.configuration.configParser.entities.selectors.output.types.ListOutput;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParserEngine implements LineProcessor {
    Entry entry;


    String regex = "\\[(.*?)\\]";

    public ParserEngine(Entry entry) {
        this.entry = entry;
    }


    @Override
    public void process(String line) {
        if (entry.getEntity().matches("^Agent\\*$")) { // de modificat sa faca match linia de log cu ce am in entry
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

        } else {
            if (entry.getEntity().matches("^\\*$")) {
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


            } else {
                if (entry.getEntity().matches("^Pylon\\*$")) {
                    Pattern pattern = Pattern.compile(this.regex);
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String matchedContent = matcher.group(1).trim();
                        if (entry.getEntity().toLowerCase().substring(0, entry.getEntity().length() - 1).equals(matchedContent.toLowerCase().substring(0, matchedContent.length() - 1))) {
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
                } else {
                    Pattern pattern = Pattern.compile(this.regex);
                    Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        String matchedContent = matcher.group(1).trim();
                        if (entry.getEntity().equalsIgnoreCase(matchedContent)) {
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

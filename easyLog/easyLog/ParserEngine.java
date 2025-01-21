package easyLog;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import easyLog.configuration.entry.Entry;
import easyLog.configuration.entry.selector.output.OutputElement;
import easyLog.configuration.entry.selector.output.types.ListOutput;

public class ParserEngine {
    Entry entry;

    String regex = "\\[(.*?)\\]";

    public ParserEngine(Entry entry) {
        this.entry = entry;
    }

    //functia process trebuie rescrisa cu selectarea patternului de procesare a logului in functie de entitate.

    //ACUM: iau aceeasi linie si o verific pt fiecare entitate in parte -> resurse consumate
    //VIITOR: fac cate o functie specializata de entry care verifica linia!!!!!

    public void processNew(String line)
    {
        entry.getProcessor().process(line);
    }




    public void process(String line) {
//        if (entry.getEntity().matches("^Agent\\*$")) { // de modificat sa faca match linia de log cu ce am in entry
//            Pattern pattern = Pattern.compile(this.regex);
//            Matcher matcher = pattern.matcher(line);
//            if (matcher.find()) {
//                String matchedContent = matcher.group(1).trim();
//                if (entry.getEntity().toLowerCase().substring(0, entry.getEntity().length() - 1).equals(matchedContent.toLowerCase().substring(0, matchedContent.length() - 2))) {
//                    if (entry.getLevel().getType() != null) {
//                        if (line.startsWith(entry.getLevel().getType())) {
//                            if (entry.getStateMatcher().getKeywords() != null) {
//                                verifyMatch(line, matchedContent);
//                            } else //count only the logs that contain the level
//                            {
//                                entry.getExpect().addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
//                                List<OutputElement> elementList = entry.getOutputItem().getElements();
//                                for (OutputElement obj : elementList) {
//                                    if (obj instanceof ListOutput) {
//                                        obj.addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
//                                    }
//                                }
//                            }
//                        }
//                    } else {
//                        if (entry.getStateMatcher().getKeywords() != null) {
//                            verifyMatch(line, matchedContent);
//                        }
//                    }
//                }
//            }
//
//        } else {
//            if (entry.getEntity().matches("^\\*$")) {
//                if (entry.getLevel().getType() != null) {
//                    if (line.startsWith(entry.getLevel().getType())) {
//                        if (entry.getStateMatcher().getKeywords() != null) {
//                            verifyMatch(line, "*");
//                        } else //count only the logs that contain the level
//                        {
//                            entry.getExpect().addMatch(line, "*", entry.getLevel().getType(), entry.getLevel());
//                            List<OutputElement> elementList = entry.getOutputItem().getElements();
//                            for (OutputElement obj : elementList) {
//                                if (obj instanceof ListOutput) {
//                                    obj.addMatch(line, "*", entry.getLevel().getType(), entry.getLevel());
//                                }
//                            }
//                        }
//
//                    }
//                } else {
//                    if (entry.getStateMatcher().getKeywords() != null) {
//                        verifyMatch(line, "*");
//                    }
//                }
//
//
//            } else {
//                if (entry.getEntity().matches("^Pylon\\*$")) {
//                    Pattern pattern = Pattern.compile(this.regex);
//                    Matcher matcher = pattern.matcher(line);
//                    if (matcher.find()) {
//                        String matchedContent = matcher.group(1).trim();
//                        if (entry.getEntity().toLowerCase().substring(0, entry.getEntity().length() - 1).equals(matchedContent.toLowerCase().substring(0, matchedContent.length() - 1))) {
//                            if (entry.getLevel().getType() != null) {
//                                if (line.startsWith(entry.getLevel().getType())) {
//                                    if (entry.getStateMatcher().getKeywords() != null) {
//                                        verifyMatch(line, matchedContent);
//                                    } else //count only the logs that contain the level
//                                    {
//                                        entry.getExpect().addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
//                                        List<OutputElement> elementList = entry.getOutputItem().getElements();
//                                        for (OutputElement obj : elementList) {
//                                            if (obj instanceof ListOutput) {
//                                                obj.addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
//                                            }
//                                        }
//                                    }
//
//                                }
//                            } else {
//                                if (entry.getStateMatcher().getKeywords() != null) {
//                                    verifyMatch(line, matchedContent);
//                                }
//                            }
//                        }
//                    }
//                } else {
//                    Pattern pattern = Pattern.compile(this.regex);
//                    Matcher matcher = pattern.matcher(line);
//                    if (matcher.find()) {
//                        String matchedContent = matcher.group(1).trim();
//                        if (entry.getEntity().equalsIgnoreCase(matchedContent)) {
//                            if (entry.getLevel().getType() != null) {
//                                if (line.startsWith(entry.getLevel().getType())) {
//                                    if (entry.getStateMatcher().getKeywords() != null) {
//                                        verifyMatch(line, matchedContent);
//                                    } else //count only the logs that contain the level
//                                    {
//                                        entry.getExpect().addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
//                                        List<OutputElement> elementList = entry.getOutputItem().getElements();
//                                        for (OutputElement obj : elementList) {
//                                            if (obj instanceof ListOutput) {
//                                                obj.addMatch(line, matchedContent, entry.getLevel().getType(), entry.getLevel());
//                                            }
//                                        }
//                                    }
//
//                                }
//                            } else {
//                                if (entry.getStateMatcher().getKeywords() != null) {
//                                    verifyMatch(line, matchedContent);
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
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

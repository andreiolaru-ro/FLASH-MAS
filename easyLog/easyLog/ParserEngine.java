package easyLog;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Node;

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
		if(("*".equals(entry.getEntity())) || line.matches(".*\\[ " + entry.getEntity() + "\\s+\\].*")) {
			// face match orice e [ ] ca regex
            if(entry.getFsm() != null)
            {
                verifyFSM(line);
            }
            if (entry.getLevel().getType() != null) {
				if(line.trim().startsWith(entry.getLevel().getType())) {
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

    public void verifyFSM(String line)
    {
        if(this.entry.getFsm().getCurrentState() == null)
        {
            throw new IllegalStateException("FSM initial state not set.");
        }
        Node current = this.entry.getFsm().getCurrentState();
        for(int i = 0 ; i < current.leavingEdges().count(); i++){
            Edge edge = current.getLeavingEdge(i);
            String regexEdge = (String) edge.getAttribute("label");// de facut ceva cu regex ca sa intre in if
//            String eventRegex = Pattern.quote(regexEdge);
            Pattern pattern = Pattern.compile(regexEdge); // imi transforma regexul intr-un string sa faca match asa cum e el (regexul)
            Matcher matcher = pattern.matcher(line);
//            if (regexEdge != null && line.toLowerCase().matches(event.toLowerCase())) {
//                System.out.println("[FSM] Detected event '" + regexEdge + "' from log line. Transitioning...");
//                this.entry.getFsm().triggerEvent(regexEdge);
//                break; // stop after first match
//            }
            if(matcher.find()){
                System.out.println("[FSM] Detected event '" + regexEdge + "' from log line. Transitioning...");
                this.entry.getFsm().triggerEvent(regexEdge);
                break; // stop after first match
            }
        }
    }
}

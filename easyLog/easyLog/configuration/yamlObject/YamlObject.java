package easyLog.configuration.yamlObject;

import static easyLog.configuration.entry.selector.expect.Expect.ExpectType.COUNT;
import static easyLog.configuration.entry.selector.expect.Expect.ExpectType.MATCH;
import static easyLog.configuration.entry.selector.output.OutputListType.*;

import java.util.ArrayList;
import java.util.List;

import easyLog.configuration.entry.Entry;
import easyLog.configuration.entry.selector.expect.ExpectCount;
import easyLog.configuration.entry.selector.expect.ExpectMatch;
import easyLog.configuration.entry.selector.level.Level;
import easyLog.configuration.entry.selector.output.OutputElement;
import easyLog.configuration.entry.selector.output.OutputItem;
import easyLog.configuration.entry.selector.output.types.ExpectOutput;
import easyLog.configuration.entry.selector.output.types.ListOutput;
import easyLog.configuration.entry.selector.output.types.StringOutput;
import easyLog.configuration.entry.selector.stateMatcher.StateMatcher;

public class YamlObject {
    private String e;
    private String level;
    private List<String> match;
    private List<String> out;
    private List<String> expect;
    private String comment;

    public YamlObject() {

    }

    public YamlObject(String e, String level, List<String> match, List<String> out, List<String> expect, String comment) {
        this.e = e;
        this.level = level;
        this.match = match;
        this.out = out;
        this.expect = expect;
        this.comment = comment;
    }

    public String getE() {
        return e;
    }

    public void setE(String e) {
        this.e = e;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getMatch() {
        return match;
    }

    public void setMatch(List<String> match) {
        this.match = match;
    }

    public List<String> getOut() {
        return out;
    }

    public void setOut(List<String> out) {
        this.out = out;
    }

    public List<String> getExpect() {
        return expect;
    }

    public void setExpect(List<String> expect) {
        this.expect = expect;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Entry initializeEntity() // function that transforms a yaml object into an entity
    {
        List<OutputElement> outputElements;
        if (this.expect.get(0).equals(COUNT.toString().toLowerCase())) {
            if(this.out!=null)
            {
                ExpectCount expectCount = new ExpectCount(this.expect.subList(1,this.expect.size()));
                //logic to build the OutputItem
                outputElements = buildOutputWithCount(this.out, expectCount);
                return new Entry(this.e, new Level(this.level), new StateMatcher(this.match), expectCount, new OutputItem(outputElements), this.comment);
            }
            return new Entry(this.e,new Level(this.level),new StateMatcher(this.match),new ExpectCount(this.expect.subList(1,this.expect.size())),this.comment);

        } else
        {
            if (this.expect.get(0).equals(MATCH.toString().toLowerCase())) {
                if(this.out!=null)
                {
                    outputElements = buildOutputWithoutCount(this.out);
                    return new Entry(this.e, new Level(this.level), new StateMatcher(this.match), new ExpectMatch(),new OutputItem(outputElements), this.comment);
                }
                return new Entry(this.e,new Level(this.level),new StateMatcher(this.match),new ExpectMatch(), this.comment);
            }
            else {
                return new Entry();
            }
        }
    }

    public List<OutputElement> buildOutputWithCount(List<String> out, ExpectCount expectCount) {
        List<OutputElement> outputElements = new ArrayList<>();
        for (String item : out) {
            if (item.equals(COUNT.toString().toLowerCase())) {
                outputElements.add(new ExpectOutput(expectCount));
            } else {
                simpleBuild(outputElements, item);
            }
        }
        return outputElements;
    }

    public List<OutputElement> buildOutputWithoutCount(List<String> out)
    {
        List<OutputElement> outputElements = new ArrayList<>();
        for (String item : out) {
            simpleBuild(outputElements, item);
        }
        return outputElements;
    }

    public void simpleBuild(List<OutputElement> outputElements, String item) {
        if (item.startsWith("list")) {
            String[] parts = item.split(":");
            if (parts[1].equals(ENTITIES.toString().toLowerCase())) {
                outputElements.add(new ListOutput(ENTITIES));
            } else {
                if (parts[1].equals(MATCHES.toString().toLowerCase())) {
                    outputElements.add(new ListOutput(MATCHES));
                } else {
                    outputElements.add(new ListOutput(LINE));
                }
            }
        } else {
            outputElements.add(new StringOutput(item));
        }
    }

    @Override
    public String toString() {
        return "YamlObject{" +
                "e='" + e + '\'' +
                ", level='" + level + '\'' +
                ", match=" + match +
                ", out=" + out +
                ", expect=" + expect +
                ", comment='" + comment + '\'' +
                '}';
    }
}

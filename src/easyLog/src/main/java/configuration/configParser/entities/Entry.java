package easyLog.src.main.java.configuration.configParser.entities;

import java.util.List;

import  easyLog.src.main.java.configuration.configParser.entities.selectors.expect.Expect;
import  easyLog.src.main.java.configuration.configParser.entities.selectors.expect.ExpectCount;
import  easyLog.src.main.java.configuration.configParser.entities.selectors.level.Level;
import  easyLog.src.main.java.configuration.configParser.entities.selectors.output.OutputElement;
import  easyLog.src.main.java.configuration.configParser.entities.selectors.output.OutputItem;
import  easyLog.src.main.java.configuration.configParser.entities.selectors.stateMatcher.StateMatcher;

public class Entry {


    public interface Recorder {
        public void build();

        /**
         * @param logLine - the entire log line that matched the entry.
         * @param entity  - the entity that produced the log line
         * @param match   - the string in the log line that matched the regexp in the <code>match</code> field
         * @param level   - the level of the log line.
         */
        public void addMatch(String logLine, String entity, String match, Level level);

    }

    private String entity;
    private Level level;

    private StateMatcher stateMatcher;

    private OutputItem outputItem;

    private Expect expect;

    private String comment;

    public Entry() {

    }

    public Entry(String entity) {
        this.entity = entity;
    }

    public Entry(String entity, Level level) {
        this.entity = entity;
        this.level = level;
    }

    public Entry(String entity, Level level, StateMatcher stateMatcher) {
        this.entity = entity;
        this.level = level;
        this.stateMatcher = stateMatcher;
    }

    public Entry(String entity, Level level, StateMatcher stateMatcher, Expect expect) {
        this.entity = entity;
        this.level = level;
        this.stateMatcher = stateMatcher;
        this.expect = expect;
    }

    public Entry(String entity, Level level, StateMatcher stateMatcher, Expect expect, OutputItem outputItem, String comment) {
        this.entity = entity;
        this.level = level;
        this.stateMatcher = stateMatcher;
        this.expect = expect;
        this.outputItem = outputItem;
        this.comment = comment;
    }

    public Entry(String entity, Level level, StateMatcher stateMatcher, Expect expect, String comment) {
        this.entity = entity;
        this.level = level;
        this.stateMatcher = stateMatcher;
        this.expect = expect;
        this.comment = comment;
    }


    public Entry(String e, Level level, StateMatcher stateMatcher, String comment) {
    }

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public StateMatcher getStateMatcher() {
        return stateMatcher;
    }

    public void setStateMatcher(StateMatcher stateMatcher) {
        this.stateMatcher = stateMatcher;
    }

    public Expect getExpect() {
        return expect;
    }

    public void setExpect(Expect expect) {
        this.expect = expect;
    }

    public OutputItem getOutputItem() {
        return outputItem;
    }

    public void setOutputItem(OutputItem outputItem) {
        this.outputItem = outputItem;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}

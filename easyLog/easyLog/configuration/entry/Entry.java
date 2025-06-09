package easyLog.configuration.entry;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.stream.Collectors;

import easyLog.LineProcessor;
import easyLog.configuration.entry.selector.expect.Expect;
import easyLog.configuration.entry.selector.level.Level;
import easyLog.configuration.entry.selector.output.OutputItem;
import easyLog.configuration.entry.selector.stateMatcher.StateMatcher;

public class Entry {
	
	public interface OutputBlockAccess {
		void addOutputElement(String element);
	}
	
	public static class OutputBlock extends LinkedList<String> {
		/**
		 * The serial UID.
		 */
		private static final long	serialVersionUID	= 1L;
		String						defaultSeparator	= " ";
		public static final String	LINE_SEPARATOR		= "\n";
		
		public OutputBlock() {
		}
		
		public OutputBlock(String separator) {
			defaultSeparator = separator;
		}
		
		public OutputBlockAccess getAccesor() {
			return new OutputBlockAccess() {
				@Override
				public void addOutputElement(String element) {
					OutputBlock.this.add(element);
				}
			};
		}
		
		String assemble(String separator) {
			return String.join(separator, this);
		}
		
		@Override
		public String toString() {
			return assemble(defaultSeparator);
		}
		
		public int getMaxLineWidth() {
			return Arrays.asList(toString().split(LINE_SEPARATOR)).stream().mapToInt(String::length).max().orElse(0);
		}
		
		public String toString(String linePrefix) {
			return String.join(LINE_SEPARATOR, Arrays.asList(toString().split(LINE_SEPARATOR)).stream()
					.map(s -> linePrefix + s).collect(Collectors.toList()));
		}
	}
	
	public interface Recorder {
		public void build(OutputBlockAccess oneLineOutput, OutputBlockAccess blockOutput);
		
		/**
		 * @param logLine
		 *            - the entire log line that matched the entry.
		 * @param entity
		 *            - the entity that produced the log line
		 * @param match
		 *            - the string in the log line that matched the regexp in the <code>match</code> field
		 * @param level
		 *            - the level of the log line.
		 */
		public void addMatch(String logLine, String entity, String match, Level level);
		
	}
	
	private String	entity;
	private Level	level;
	
	private StateMatcher stateMatcher;
	
	private OutputItem outputItem;
	
	private Expect expect;
	
	private LineProcessor	processor;
	private String			comment;
	
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
	
	public Entry(String entity, Level level, StateMatcher stateMatcher, Expect expect, OutputItem outputItem,
			String comment) {
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
	
	public LineProcessor getProcessor() {
		return processor;
	}
	
	public void setProcessor(LineProcessor processor) {
		this.processor = processor;
	}
}

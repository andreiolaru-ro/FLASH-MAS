package easyLog.configuration.entry.selector.output;

public enum OutputListType {
    /**
     * prints entities that logged the matching lines
     */
    ENTITIES,
    /**
     * prints only the string that matched the regexp in the "match" field
     */
    MATCHES,
    /**
     * prints the entire log line that matched
     */
    LINE
}

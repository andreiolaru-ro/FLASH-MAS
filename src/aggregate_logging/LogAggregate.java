package aggregate_logging;

public interface LogAggregate {
    String name();
    public void register(String entityName, Object... args);
    public void clear();
    public void printSummary();
}


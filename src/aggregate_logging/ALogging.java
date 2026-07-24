package aggregate_logging;

import abms.wolfSheepPredation.WolfSheepGroup;

import java.util.HashSet;
import java.util.Set;

public class ALogging {
    private static final ALogging instance = new ALogging();

    private final Set<LogAggregate> activeLogs = new HashSet<>();
    public static WolfSheepLog testingLog = WolfSheepLog.SHEEP_EATS_GRASS;
    private ALogging() {}

    public static ALogging getInstance() {
        return instance;
    }

    public void li_agr(LogAggregate log, String entityName, Object... args) {
        if (log != null) {
            this.activeLogs.add(log);
            log.register(entityName, args);
        }
    }

    public void print_agr(LogAggregate log) {
        if (log != null)
            log.printSummary();
    }
    public void clear_agr(LogAggregate log) {
        if (log != null)
            log.clear();
    }
    public void printAllSummaries() {
        for (LogAggregate log : this.activeLogs) {
            log.printSummary();
        }
    }

    public void clearAll() {
        for (LogAggregate log : this.activeLogs) {
            log.clear();
        }
        this.activeLogs.clear();
    }
}
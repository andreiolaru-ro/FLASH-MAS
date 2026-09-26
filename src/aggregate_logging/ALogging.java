package aggregate_logging;

import abms.wolfSheepPredation.WolfSheepGroup;
import net.xqhs.flash.core.Entity;

import java.util.HashMap;
import java.util.Map;

public class ALogging {
    private static final ALogging instance = new ALogging();
    private final Map<LogAggregate, Map<String, MessageSummary>> logs = new HashMap<>();
    private ALogging() {}

    public static ALogging getInstance() {
        return instance;
    }

    public void li_agr(LogAggregate log,  Entity<?> entity, String msg, Object... args) {
        if (log == null) return;
        Map<String, MessageSummary> logMap = logs.computeIfAbsent(log, k -> new HashMap<>());
        MessageSummary summary = logMap.computeIfAbsent(msg, k -> new MessageSummary(msg, args.length));
        summary.register(entity, args);
    }

    public void print_agr(LogAggregate log) {
        if (log == null)
            return;

        Map<String, MessageSummary> logMap = logs.get(log);
        if (logMap == null)
            return;

        for (MessageSummary summary : logMap.values()) {
            summary.printSummary();
        }
    }

    public void printAllAgr() {
        for (Map.Entry<LogAggregate, Map<String, MessageSummary>> entry : logs.entrySet()) {
            LogAggregate log = entry.getKey();
            Map<String, MessageSummary> logMap = entry.getValue();
            System.out.println("-----" + log.name() + "-----");

            for (MessageSummary summary : logMap.values()) {
                summary.printSummary();
            }
            System.out.println();
        }
    }

    public void clear_agr(LogAggregate log) {
        if (log == null) return;

        Map<String, MessageSummary> logMap = logs.get(log);
        if (logMap == null) return;

        for (MessageSummary summary : logMap.values()) {
            summary.clear();
        }
    }

    public void clearAll() {
        for (Map<String, MessageSummary> logMap : logs.values()) {
            for (MessageSummary summary : logMap.values()) {
                summary.clear();
            }
        }
    }
}
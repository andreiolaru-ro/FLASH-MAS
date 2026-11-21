package aggregate_logging;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.Entity;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.UnitComponent;

public class ALog {
	protected static boolean												NORMAL_OUTPUT	= true;
    protected static final UnitComponent DefaultLogger = new UnitComponent("agr-log");
	
	protected static final Map<LogAggregate, Map<String, MessageSummary>>	logs			= new HashMap<>();
	
	

    public static void li(Logger logger, LogAggregate log,  Entity<?> entity, String msg, Object... args) {
        if (log == null) return;
        Map<String, MessageSummary> logMap = logs.computeIfAbsent(log, k -> new HashMap<>());
        MessageSummary summary = logMap.computeIfAbsent(msg, k -> new MessageSummary(msg, args.length));
        summary.register(entity, args);
		if(NORMAL_OUTPUT) {
            Logger out = logger != null ? logger : DefaultLogger;
			out.li("[] " + msg, entity != null ? entity.getName() != null ? entity.getName() : entity.toString() : "<null>",
					args);
        }
    }

    public static void printLog(LogAggregate log) {
        if (log == null)
            return;

        Map<String, MessageSummary> logMap = logs.get(log);
        if (logMap == null)
            return;

        for (MessageSummary summary : logMap.values()) {
            summary.printSummary();
        }
    }

    public static void printAllLogs() {
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

    public static void clearLog(LogAggregate log) {
        if (log == null) return;

        Map<String, MessageSummary> logMap = logs.get(log);
        if (logMap == null) return;

        for (MessageSummary summary : logMap.values()) {
            summary.clear();
        }
    }

    public static void clearAllLogs() {
        for (Map<String, MessageSummary> logMap : logs.values()) {
            for (MessageSummary summary : logMap.values()) {
                summary.clear();
            }
        }
    }
}
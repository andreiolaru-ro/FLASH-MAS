package aggregate_logging;

import java.util.HashMap;
import java.util.Map;

import net.xqhs.flash.core.Entity;
import net.xqhs.util.logging.Unit;

public class ALog extends Unit {
	protected boolean												NORMAL_OUTPUT	= true;
	protected static final ALog									instance		= new ALog();
	protected final Map<LogAggregate, Map<String, MessageSummary>>	logs			= new HashMap<>();
	
	protected ALog() {
		setUnitName("agr-log");
	}

    public static ALog getInstance() {
        return instance;
    }

    public void li_agr(LogAggregate log,  Entity<?> entity, String msg, Object... args) {
        if (log == null) return;
        Map<String, MessageSummary> logMap = logs.computeIfAbsent(log, k -> new HashMap<>());
        MessageSummary summary = logMap.computeIfAbsent(msg, k -> new MessageSummary(msg, args.length));
        summary.register(entity, args);
		if(NORMAL_OUTPUT)
			li("[] " + msg, entity != null ? entity.getName() != null ? entity.getName() : entity.toString() : "<null>",
					args);
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
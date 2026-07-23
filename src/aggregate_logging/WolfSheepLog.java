package aggregate_logging;

import java.util.HashSet;
import java.util.Set;

// User domain
public enum WolfSheepLog implements LogAggregate {
    SHEEP_EATS_GRASS("sheep eats grass [] at coordinates []"),
    WOLF_MOVES("wolf moved to []");

    private final String messageTemplate;

    private final Set<String> uniqueEntities = new HashSet<>();
    private int callCount = 0; // Each constant gets its own counter too

    WolfSheepLog(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    @Override
    public void register(String entityName, Object... args) {
        this.uniqueEntities.add(entityName);
        this.callCount++;
    }

    @Override
    public void printSummary() {
        System.out.println("Log: " + this.name());
        System.out.println("Template: " + this.messageTemplate);
        System.out.println("Unique entities involved: " + this.uniqueEntities.size());
        System.out.println("Total actions: " + this.callCount);
        System.out.println("-------------------------");
    }

    @Override
    public void clear() {
        this.uniqueEntities.clear();
        this.callCount = 0;
    }
}
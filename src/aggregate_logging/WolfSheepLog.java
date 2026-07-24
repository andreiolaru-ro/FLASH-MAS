package aggregate_logging;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public enum WolfSheepLog implements LogAggregate {
    SHEEP_EATS_GRASS("sheep eats grass [] at coordinates []"),
    SHEEP_STEP("sheep step"),
    SHEEP_EATEN("sheep eaten and deregistered"),
    SHEEP_RECEIVED_ALERT("sheep received danger alert from neighbor"),
    SHEEP_SPOTS_WOLF("sheep spot a wolf nearby and broadcast"),
    SHEEP_RUNS_AWAY("sheep ran away"),
    WOLF_MOVES("wolf moved to []");

    private final String messageTemplate;

    private final Set<String> uniqueEntities = new HashSet<>();
    private final ArrayList<Set<String>> uniqueFields = new ArrayList<>();
    private int callCount = 0;

    WolfSheepLog(String messageTemplate) {

        this.messageTemplate = messageTemplate;
        int nrUniqueFields = messageTemplate.split("\\[\\]", -1).length - 1;
        for (int i = 0; i < nrUniqueFields; i++)
            uniqueFields.add(new HashSet<>());
    }

    @Override
    public void register(String entityName, Object... args) {
        uniqueEntities.add(entityName);
        for (int i = 0; i < args.length; i++)
            uniqueFields.get(i).add(args[i].toString());
        callCount++;
    }
    private String filledTemplate() {
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;
        for (Set<String> uniqueField : uniqueFields) {
            int bracketIndex = messageTemplate.indexOf("[]", lastIndex);
            sb.append(messageTemplate, lastIndex, bracketIndex);
            sb.append("[");
            sb.append(uniqueField.size());
            sb.append(" entries]");
            lastIndex = bracketIndex + 2;
        }
        if (lastIndex != messageTemplate.length())
            sb.append(messageTemplate, lastIndex, messageTemplate.length());
        return sb.toString();
    }
    @Override
    public void printSummary() {
        System.out.println("Total " + this.name() + " actions: " + callCount);
        System.out.println("[" + uniqueEntities.size() + " entities] "
                            + filledTemplate());
    }

    @Override
    public void clear() {
        this.uniqueEntities.clear();
        this.callCount = 0;
    }
}
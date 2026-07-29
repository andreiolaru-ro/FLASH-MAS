package aggregate_logging;

import net.xqhs.flash.core.Entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class MessageSummary {
    private final Set<String> uniqueEntities = new HashSet<>();
    private final ArrayList<Set<String>> uniqueFields = new ArrayList<>();
    private final String msg;
    private int callCount;

    MessageSummary(String msg, int arguments) {
        this.msg = msg;
        for (int i = 0; i < arguments; i++)
            uniqueFields.add(new HashSet<>());
    }

    public void register(Entity<?> entity, Object ...args) {
        uniqueEntities.add(entity.getName());
        // Safely iterate to prevent IndexOutOfBoundsException if subsequent calls pass more arguments
        int fieldsToProcess = Math.min(args.length, uniqueFields.size());
        for (int i = 0; i < fieldsToProcess; i++)
            uniqueFields.get(i).add(args[i].toString());
        callCount++;
    }

    public void printSummary() {
        System.out.println("[" + callCount + " actions | " + uniqueEntities.size() + " entities] "
                + filledPlaceholders());
    }

    public void clear() {
        uniqueEntities.clear();
        for (Set<String> field : uniqueFields)
            field.clear();
        callCount = 0;
    }

    private String filledPlaceholders() {
        StringBuilder sb = new StringBuilder();
        int lastIndex = 0;

        for (Set<String> uniqueField : uniqueFields) {
            int bracketIndex = msg.indexOf("[]", lastIndex);

            // in case there are more arguments than placeholders
            if (bracketIndex == -1)
                break;

            sb.append(msg, lastIndex, bracketIndex);
            sb.append("[");
            sb.append(uniqueField.size());
            sb.append(" entries]");
            lastIndex = bracketIndex + 2;
        }

        if (lastIndex != msg.length())
            sb.append(msg, lastIndex, msg.length());

        return sb.toString();
    }
}

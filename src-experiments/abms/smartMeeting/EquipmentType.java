package abms.smartMeeting;

import java.util.LinkedHashSet;
import java.util.Set;

public enum EquipmentType {
    PROJECTOR,
    PRINTER,
    VIDEO_CONFERENCE,
    WHITEBOARD;

    public static Set<EquipmentType> parseSet(String value) {
        Set<EquipmentType> result = new LinkedHashSet<>();
        if (value == null || value.trim().isEmpty())
            return result;
        for (String part : value.split(",")) {
            String token = part.trim();
            if (!token.isEmpty())
                result.add(EquipmentType.valueOf(token.toUpperCase()));
        }
        return result;
    }

    public static String serialize(Set<EquipmentType> equipment) {
        if (equipment == null || equipment.isEmpty())
            return "";
        StringBuilder sb = new StringBuilder();
        for (EquipmentType item : equipment) {
            if (sb.length() > 0)
                sb.append(',');
            sb.append(item.name());
        }
        return sb.toString();
    }
}

package abms.smartMeeting;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class MeetingRequest {
    private final String requestId;
    private final String requesterName;
    private final int attendees;
    private final int durationMinutes;
    private final TimeSlot preferredSlot;
    private final Set<EquipmentType> requiredEquipment;
    private final int priority;

    public MeetingRequest(String requestId, String requesterName, int attendees, int durationMinutes,
            TimeSlot preferredSlot, Set<EquipmentType> requiredEquipment, int priority) {
        this.requestId = requestId;
        this.requesterName = requesterName;
        this.attendees = attendees;
        this.durationMinutes = durationMinutes;
        this.preferredSlot = preferredSlot;
        this.requiredEquipment = new LinkedHashSet<>(requiredEquipment);
        this.priority = priority;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public int getAttendees() {
        return attendees;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public TimeSlot getPreferredSlot() {
        return preferredSlot;
    }

    public Set<EquipmentType> getRequiredEquipment() {
        return Collections.unmodifiableSet(requiredEquipment);
    }

    public int getPriority() {
        return priority;
    }
}

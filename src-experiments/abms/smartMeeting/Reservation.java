package abms.smartMeeting;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class Reservation {
    private final String reservationId;
    private final String requestId;
    private final String roomId;
    private final TimeSlot slot;
    private final Set<EquipmentType> equipment;
    private ReservationStatus status;

    public Reservation(String reservationId, String requestId, String roomId, TimeSlot slot,
            Set<EquipmentType> equipment) {
        this.reservationId = reservationId;
        this.requestId = requestId;
        this.roomId = roomId;
        this.slot = slot;
        this.equipment = new LinkedHashSet<>(equipment);
        this.status = ReservationStatus.LOCKED;
    }

    public String getReservationId() {
        return reservationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getRoomId() {
        return roomId;
    }

    public TimeSlot getSlot() {
        return slot;
    }

    public Set<EquipmentType> getEquipment() {
        return Collections.unmodifiableSet(equipment);
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void lock() {
        status = ReservationStatus.LOCKED;
    }

    public void confirm() {
        status = ReservationStatus.CONFIRMED;
    }

    public void release() {
        status = ReservationStatus.RELEASED;
    }

    public boolean conflictsWith(TimeSlot otherSlot) {
        return status != ReservationStatus.RELEASED && slot.overlaps(otherSlot);
    }
}

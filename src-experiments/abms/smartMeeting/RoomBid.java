package abms.smartMeeting;

public class RoomBid {
    private final String requestId;
    private final String roomAgentName;
    private final String roomId;
    private final boolean feasible;
    private final int score;
    private final TimeSlot proposedSlot;
    private final String reason;

    public RoomBid(String requestId, String roomAgentName, String roomId, boolean feasible, int score,
            TimeSlot proposedSlot, String reason) {
        this.requestId = requestId;
        this.roomAgentName = roomAgentName;
        this.roomId = roomId;
        this.feasible = feasible;
        this.score = score;
        this.proposedSlot = proposedSlot;
        this.reason = reason;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isFeasible() {
        return feasible;
    }

    public int getScore() {
        return score;
    }

    public String getRoomAgentName() {
        return roomAgentName;
    }

    public String getRoomId() {
        return roomId;
    }

    public TimeSlot getProposedSlot() {
        return proposedSlot;
    }

    public String getReason() {
        return reason;
    }
}

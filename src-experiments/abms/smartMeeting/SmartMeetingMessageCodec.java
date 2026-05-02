package abms.smartMeeting;

import net.xqhs.flash.core.agent.AgentWave;

public final class SmartMeetingMessageCodec {
    private static final String TYPE = "smartMeetingType";
    private static final String REQUEST_ID = "requestId";
    private static final String REQUESTER = "requester";
    private static final String ATTENDEES = "attendees";
    private static final String DURATION = "duration";
    private static final String SLOT = "slot";
    private static final String EQUIPMENT = "equipment";
    private static final String PRIORITY = "priority";
    private static final String ROOM_AGENT = "roomAgent";
    private static final String ROOM_ID = "roomId";
    private static final String FEASIBLE = "feasible";
    private static final String SCORE = "score";
    private static final String REASON = "reason";
    private static final String RESERVATION_ID = "reservationId";

    private SmartMeetingMessageCodec() {
    }

    public static AgentWave encodeRequestForProposals(MeetingRequest request) {
        AgentWave wave = baseWave(SmartMeetingMessageType.REQUEST_FOR_PROPOSALS);
        wave.add(REQUEST_ID, request.getRequestId());
        wave.add(REQUESTER, request.getRequesterName());
        wave.add(ATTENDEES, String.valueOf(request.getAttendees()));
        wave.add(DURATION, String.valueOf(request.getDurationMinutes()));
        wave.add(SLOT, request.getPreferredSlot().toString());
        wave.add(EQUIPMENT, EquipmentType.serialize(request.getRequiredEquipment()));
        wave.add(PRIORITY, String.valueOf(request.getPriority()));
        return wave;
    }

    public static AgentWave encodeBid(RoomBid bid) {
        AgentWave wave = baseWave(SmartMeetingMessageType.BID);
        addBidFields(wave, bid);
        return wave;
    }

    public static AgentWave encodeAcceptBid(RoomBid bid) {
        AgentWave wave = baseWave(SmartMeetingMessageType.ACCEPT_BID);
        addBidFields(wave, bid);
        return wave;
    }

    public static AgentWave encodeRejectBid(String requestId) {
        AgentWave wave = baseWave(SmartMeetingMessageType.REJECT_BID);
        wave.add(REQUEST_ID, requestId);
        return wave;
    }

    public static AgentWave encodeReleaseRoom(String reservationId) {
        AgentWave wave = baseWave(SmartMeetingMessageType.RELEASE_ROOM);
        wave.add(RESERVATION_ID, reservationId);
        return wave;
    }

    public static SmartMeetingMessageType decodeType(AgentWave wave) {
        String type = wave.get(TYPE);
        if (type == null)
            type = wave.getContent();
        return SmartMeetingMessageType.valueOf(type);
    }

    public static MeetingRequest decodeMeetingRequest(AgentWave wave) {
        return new MeetingRequest(
                wave.get(REQUEST_ID),
                wave.get(REQUESTER),
                readInt(wave, ATTENDEES, 1),
                readInt(wave, DURATION, 60),
                TimeSlot.parse(wave.get(SLOT)),
                EquipmentType.parseSet(wave.get(EQUIPMENT)),
                readInt(wave, PRIORITY, 1));
    }

    public static RoomBid decodeRoomBid(AgentWave wave) {
        return new RoomBid(
                wave.get(REQUEST_ID),
                wave.get(ROOM_AGENT),
                wave.get(ROOM_ID),
                Boolean.parseBoolean(wave.get(FEASIBLE)),
                readInt(wave, SCORE, 0),
                TimeSlot.parse(wave.get(SLOT)),
                wave.get(REASON));
    }

    public static String decodeReservationId(AgentWave wave) {
        return wave.get(RESERVATION_ID);
    }

    private static AgentWave baseWave(SmartMeetingMessageType type) {
        AgentWave wave = new AgentWave(type.name());
        wave.add(TYPE, type.name());
        return wave;
    }

    private static void addBidFields(AgentWave wave, RoomBid bid) {
        wave.add(REQUEST_ID, bid.getRequestId());
        wave.add(ROOM_AGENT, bid.getRoomAgentName());
        wave.add(ROOM_ID, bid.getRoomId());
        wave.add(FEASIBLE, String.valueOf(bid.isFeasible()));
        wave.add(SCORE, String.valueOf(bid.getScore()));
        wave.add(SLOT, bid.getProposedSlot().toString());
        wave.add(REASON, bid.getReason() != null ? bid.getReason() : "");
    }

    private static int readInt(AgentWave wave, String key, int fallback) {
        String value = wave.get(key);
        if (value == null)
            return fallback;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}

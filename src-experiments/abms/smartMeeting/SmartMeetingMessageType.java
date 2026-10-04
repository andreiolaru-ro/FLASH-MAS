package abms.smartMeeting;

public enum SmartMeetingMessageType {
    REQUEST_FOR_PROPOSALS,
    BID,
    ACCEPT_BID,
    REJECT_BID,
    RELEASE_ROOM,
    BOOKING_REQUEST,
    BOOKING_RESPONSE,
    // internal events, scheduled by an agent for itself through the temporal context
    AUCTION_TIMEOUT,
    RFP_RETRY,
    SEND_BOOKING_REQUEST
}

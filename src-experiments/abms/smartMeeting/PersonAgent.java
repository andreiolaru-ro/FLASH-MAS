package abms.smartMeeting;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import net.xqhs.flash.abms.EnvironmentLinkShard;
import net.xqhs.flash.abms.Simulation;
import net.xqhs.flash.abms.SteppableEntity;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.core.util.MultiTreeMap;

public class PersonAgent extends BaseAgent implements SteppableEntity, ShardContainer {
    private static final long serialVersionUID = 1L;
    private static final AgentShardDesignation ENVIRONMENT =
            AgentShardDesignation.customShard("Environment");

    private EnvironmentLinkShard e = new EnvironmentLinkShard();
    private Queue<AgentWave> incomingWaves = new LinkedList<>();

    private enum State { IDLE, WAITING_FOR_RESPONSE, DONE }

    private State state = State.IDLE;
    private String auctionAgentName;
    private EntityProxy<?> auctionAgentRef;
    private Simulation simulation;

    // Request parameter ranges (configured from the scenario JSON via the boot string).
    private int attendeesMin = 2;
    private int attendeesMax = 8;
    private int[] durations = {30, 60};
    private int startMin = 9 * 60;
    private int startMax = 12 * 60 + 30;
    private int startStep = 30;
    private int priorityMin = 1;
    private int priorityMax = 3;
    private List<EquipmentType> equipmentPool = new ArrayList<>();
    private double equipmentProb = 0.5;

    // Outcome tracking for the per-run statistics.
    private boolean responseReceived = false;
    private boolean responseAccepted = false;
    private String responseRoomId;
    private String responseReason;

    public PersonAgent() {
        e.addGeneralContext(this);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.containsKey("auctionAgent"))
            auctionAgentName = configuration.getAValue("auctionAgent");
        attendeesMin = readInt(configuration, "attendeesMin", attendeesMin);
        attendeesMax = readInt(configuration, "attendeesMax", attendeesMax);
        startMin = readInt(configuration, "startMin", startMin);
        startMax = readInt(configuration, "startMax", startMax);
        startStep = readInt(configuration, "startStep", startStep);
        priorityMin = readInt(configuration, "priorityMin", priorityMin);
        priorityMax = readInt(configuration, "priorityMax", priorityMax);
        if (configuration.containsKey("durations"))
            durations = parseIntList(configuration.getAValue("durations"));
        if (configuration.containsKey("equipmentPool"))
            equipmentPool = new ArrayList<>(EquipmentType.parseSet(configuration.getAValue("equipmentPool")));
        if (configuration.containsKey("equipmentProb"))
            equipmentProb = Double.parseDouble(configuration.getAValue("equipmentProb"));
        if (equipmentPool.isEmpty())
            equipmentPool.add(EquipmentType.PROJECTOR);
        return true;
    }

    private static int[] parseIntList(String value) {
        if (value == null || value.isEmpty()) return new int[]{30, 60};
        String[] parts = value.split(",");
        int[] out = new int[parts.length];
        for (int i = 0; i < parts.length; i++)
            out[i] = Integer.parseInt(parts[i].trim());
        return out;
    }

    private static int readInt(MultiTreeMap configuration, String key, int fallback) {
        if (configuration == null || !configuration.containsKey(key)) return fallback;
        try { return Integer.parseInt(configuration.getAValue(key)); }
        catch (NumberFormatException ex) { return fallback; }
    }

    public boolean isResponseReceived() {
        return responseReceived;
    }

    public boolean isResponseAccepted() {
        return responseAccepted;
    }

    public String getResponseRoomId() {
        return responseRoomId;
    }

    public String getResponseReason() {
        return responseReason;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        if (context instanceof Simulation)
            simulation = (Simulation) context;
        e.addGeneralContext(context);
        return super.addGeneralContext(context);
    }

    @Override
    public boolean postAgentEvent(AgentEvent event) {
        if (event.getType() == AgentEvent.AgentEventType.AGENT_WAVE && event instanceof AgentWave) {
            incomingWaves.add((AgentWave) event);
            return true;
        }
        return false;
    }

    @Override
    public AgentShard getAgentShard(AgentShardDesignation designation) {
        return ENVIRONMENT.equals(designation) ? e : null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Entity<Pylon>> EntityProxy<C> asContext() {
        return (EntityProxy<C>) this;
    }

    @Override
    public void step() {
        processIncomingWaves();

        switch (state) {
            case IDLE:
                if (auctionAgentRef == null)
                    auctionAgentRef = resolveAuctionAgent();
                if (auctionAgentRef == null)
                    return;
                MeetingRequest request = createRequest();
                e.sendDirect(auctionAgentRef, SmartMeetingMessageCodec.encodeBookingRequest(request));
                li("sent booking request [] to auction agent", request.getRequestId());
                state = State.WAITING_FOR_RESPONSE;
                break;
            case WAITING_FOR_RESPONSE:
                // just waiting — responses handled in processIncomingWaves
                break;
            case DONE:
                break;
        }
    }

    private void processIncomingWaves() {
        while (!incomingWaves.isEmpty()) {
            AgentWave wave = incomingWaves.poll();
            try {
                SmartMeetingMessageType type = SmartMeetingMessageCodec.decodeType(wave);
                if (type == SmartMeetingMessageType.BOOKING_RESPONSE) {
                    String result = wave.get("result");
                    String roomId = wave.get("roomId");
                    responseReceived = true;
                    responseAccepted = "accepted".equals(result);
                    responseRoomId = roomId;
                    responseReason = wave.get("reason");
                    if (responseAccepted)
                        li("booking CONFIRMED for room []", roomId);
                    else
                        li("booking FAILED: []", wave.get("reason"));
                    state = State.DONE;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private MeetingRequest createRequest() {
        int attendeesRange = Math.max(1, attendeesMax - attendeesMin + 1);
        int attendees = attendeesMin + e.nextInt(attendeesRange);
        int duration = durations[e.nextInt(durations.length)];
        int slotsCount = Math.max(1, ((startMax - startMin) / Math.max(1, startStep)) + 1);
        int start = startMin + (e.nextInt(slotsCount) * startStep);
        Set<EquipmentType> equipment = new LinkedHashSet<>();
        for (EquipmentType item : equipmentPool)
            if (e.nextDouble() < equipmentProb)
                equipment.add(item);
        int priorityRange = Math.max(1, priorityMax - priorityMin + 1);
        int priority = priorityMin + e.nextInt(priorityRange);
        return new MeetingRequest("REQ-" + getEntityName(), getEntityName(), attendees, duration,
                new TimeSlot(start, start + duration), equipment, priority);
    }

    private EntityProxy<?> resolveAuctionAgent() {
        if (auctionAgentName == null || simulation == null)
            return null;
        for (Entity<?> entity : simulation.getSimulationObjects())
            if (entity instanceof AuctionAgent && auctionAgentName.equals(entity.asContext().getEntityName()))
                return entity.asContext();
        return null;
    }

    @Override
    public String getEntityName() {
        return getName() != null ? getName() : "Person";
    }
}

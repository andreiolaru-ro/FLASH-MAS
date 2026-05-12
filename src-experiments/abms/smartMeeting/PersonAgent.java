package abms.smartMeeting;

import java.util.LinkedHashSet;
import java.util.LinkedList;
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

    public PersonAgent() {
        e.addGeneralContext(this);
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        if (configuration.containsKey("auctionAgent"))
            auctionAgentName = configuration.getAValue("auctionAgent");
        return true;
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
                    if ("accepted".equals(result))
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
        int attendees = 2 + e.nextInt(7);
        int duration = e.nextBoolean() ? 30 : 60;
        int start = (9 * 60) + (e.nextInt(8) * 30);
        Set<EquipmentType> equipment = new LinkedHashSet<>();
        if (e.nextBoolean())
            equipment.add(EquipmentType.PROJECTOR);
        return new MeetingRequest("REQ-" + getEntityName(), getEntityName(), attendees, duration,
                new TimeSlot(start, start + duration), equipment, 1 + e.nextInt(3));
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

package httphomeserver;

import httphomeserver.RegionsHttpMessageFactory.MessageType;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobilityAwareMessagingShard;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.NameBasedMessagingShard;
import net.xqhs.flash.core.util.PlatformUtils;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import wsRegions.MessageFactory;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import static httphomeserver.RegionsHttpMessageFactory.MessageType.CONTENT;

public class RegionsHttpMessagingShard extends AbstractMessagingShard implements MobilityAwareMessagingShard {

    /**
     * The serial UID.
     */
    private static final long serialVersionUID = 10L;

    @Override
    protected void receiveMessage(String source, String destination, String str) {
        Object obj = JSONValue.parse(str);
        JSONObject message = (JSONObject) obj;
        String type = (String) message.get("type");
        messageTriggeredBehavior(source, destination, type, (String) message.get("content"));
    }
    transient String													nextMoveTarget;
    LinkedBlockingQueue<Map.Entry<Map.Entry<String, String>, String>> inQueue;
    LinkedBlockingQueue<String>											outQueue;

    public void startShard(MessageType connection_type) {
        setUnitName(getAgent().getEntityName());
        setLoggerType(PlatformUtils.platformLogType());
        while(inQueue != null && !inQueue.isEmpty()) {
            Map.Entry<Map.Entry<String, String>, String> entry = inQueue.poll();
            if(inQueue.isEmpty())
                inQueue = null;
            Map.Entry<String, String> src_dest = entry.getKey();
            lf("Managing queued message from []: []", src_dest.getKey(), entry.getValue());
            super.receiveMessage(src_dest.getKey(), src_dest.getValue(), entry.getValue());
        }
        inQueue = null;
        pylon.send(null, null, RegionsHttpMessageFactory.createMessage(pylon.getEntityName(), this.getName(), connection_type, null));
        while(outQueue != null && !outQueue.isEmpty())
            pylon.send(null, null, outQueue.poll());
        outQueue = null;
        lf("completed startup procedure.");
    }

    @Override
    public String getName() {
        return getAgent().getEntityName();
    }

    @Override
    public String getAgentAddress() {
        return this.getName();
    }

    @Override
    public boolean sendMessage(String source, String target, String content) {
        Map<String, String> data = new HashMap<>();
        data.put("destination", target);
        data.put("content", content);
        String message = RegionsHttpMessageFactory.createMessage(pylon.getEntityName(), this.getName(),
                // FIXME: very ugly hack, may fail easily
                source.contains("node") || target.contains("node") ? MessageType.AGENT_CONTENT : CONTENT,
                data);
        li("Send message [] from [] to [] []", content, source, target,
                outQueue != null ? "will queue" : "will not queue");
        if(outQueue != null) {
            outQueue.add(message);
            return true;
        }

        if(target.contains("Monitoring")) {
            // FIXME: does this actually occur anymore?
            return true;
        }
        pylon.send(source, target, message);
        return true;
    }

    @Override
    public String extractAgentAddress(String endpoint) {
        return Helper.extractAgentAddress(endpoint);
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        switch(event.getType()) {
            case AGENT_START:
                if(event.get(CompositeAgent.TRANSIENT_EVENT_PARAMETER) != null) {
                    li("Agent started after move. Queued messages: [] in / [] out", inQueue.size(), outQueue.size());
                    startShard(MessageType.CONNECT);
                }
                else {
                    this.register(getAgent().getEntityName());
                    startShard(MessageType.REGISTER);
                }
                getAgent().postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AFTER_MOVE));
                break;
            case BEFORE_MOVE:
                // create queues, no messages sent / received beyond this point
                inQueue = new LinkedBlockingQueue<>();
                outQueue = new LinkedBlockingQueue<>();

                nextMoveTarget = event.get("TARGET");
                lf("Agent " + this.getName() + " wants to move to another node " + nextMoveTarget);
                pylon.send(null, null, RegionsHttpMessageFactory.createMessage(pylon.getEntityName(), this.getName(), MessageType.REQ_LEAVE, null));
                break;
            case AFTER_MOVE:
                // String entityName = getAgent().getEntityName();
                // String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null,
                // String.valueOf(new Timestamp(System.currentTimeMillis())));
                // pylon.send(this.getName(), pylon.getEntityName(), notify_content);

                // pylon.register(entityName, inbox);
                // wsClient.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONNECT, null));
                break;
        }
    }

    protected void messageTriggeredBehavior(String source, String destination, String type, String content) {
        MessageType messageType = type == null ? CONTENT : MessageType.valueOf(type);
        switch(messageType) {
            case CONTENT:
                content = RegionsHttpMessageFactory.createMonitorNotification(RegionsHttpMessageFactory.ActionType.RECEIVE_MESSAGE,
                        content, String.valueOf(new Timestamp(System.currentTimeMillis())));
                li("Message from []: [] []", source, content,
                        inQueue != null ? "will queue" : "will not queue");
                super.receiveMessage(source, destination, content);
                break;
            case REQ_ACCEPT:
                li("Prepared to leave. Queued messages: [] in / [] out", inQueue.size(), outQueue.size());

                getAgent().postAgentEvent((AgentEvent) new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP)
                        .add(CompositeAgent.TRANSIENT_EVENT_PARAMETER, MobileCompositeAgent.MOVE_TRANSIENT_EVENT_PARAMETER)
                        .add(MobileCompositeAgent.TARGET, nextMoveTarget));
                nextMoveTarget = null;
                break;
            case AGENT_CONTENT:
                li("Received agent from " + source);
                AgentEvent arrived_agent = new AgentWave();
                arrived_agent.add("content", content);
                arrived_agent.add("destination-complete", destination);
                getAgent().postAgentEvent(arrived_agent);
                break;
            default:
                le("Unknown type");
        }
    }
}

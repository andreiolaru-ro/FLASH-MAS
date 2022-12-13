package wsRegions;

import static wsRegions.MessageFactory.createMessage;
import static wsRegions.MessageFactory.createMonitorNotification;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobilityAwareMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.NameBasedMessagingShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import wsRegions.MessageFactory.MessageType;

public class WSRegionsShard extends NameBasedMessagingShard
		implements MobilityAwareMessagingShard {
	/**
	 * the Websocket object connected to Region server.
	 */
	transient protected WSClient										wsClient;
	transient URI														serverURI;
	transient String													nextMoveTarget;
	LinkedBlockingQueue<Map.Entry<Map.Entry<String, String>, String>>	inQueue;
	LinkedBlockingQueue<String>											outQueue;
	
	@Override
	protected MessageReceiver buildMessageReceiver() {
		return new MessageReceiver() {
			@Override
			public void receive(String source, String destination, String content) {
				receiveMessage(source, destination, content);
			}
		};
	}
	
	public void startShadowAgentShard(MessageType connection_type) {
		setUnitName(getAgent().getEntityName());
		setLoggerType(PlatformUtils.platformLogType());
		wsClient = new WSClient(serverURI, 10, 5000, this.getLogger()) {
			@Override
			public void onMessage(String s) {
				li("[]/[]", inQueue, outQueue);
				messageTriggeredBehavior(s);
			}
		};
		while(inQueue != null && !inQueue.isEmpty()) {
			Entry<Entry<String, String>, String> entry = inQueue.poll();
			if(inQueue.isEmpty())
				inQueue = null;
			Entry<String, String> src_dest = entry.getKey();
			lf("Managing queued message from []: []", src_dest.getKey(), entry.getValue());
			super.receiveMessage(src_dest.getKey(), src_dest.getValue(), entry.getValue());
		}
		inQueue = null;
		wsClient.send(createMessage(pylon.getEntityName(), this.getName(), connection_type, null));
		while(outQueue != null && !outQueue.isEmpty())
			wsClient.send(outQueue.poll());
		outQueue = null;
		lf("completed startup procedure.");
	}
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		if(configuration.getAValue("connectTo") != null) {
			try {
				this.serverURI = new URI(configuration.getAValue("connectTo"));
			} catch(URISyntaxException e) {
				le("Incorrect URI format []", configuration.getAValue("connectTo"));
				return false;
			}
		}
		return true;
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
		// String notify_content = createMonitorNotification(ActionType.SEND_MESSAGE, content,
		// String.valueOf(new Timestamp(System.currentTimeMillis())));
		// pylon.send(this.getName(), target, notify_content);
		
		Map<String, String> data = new HashMap<>();
		data.put("destination", target);
		data.put("content", content);
		String message = createMessage(pylon.getEntityName(), this.getName(),
				// FIXME: very ugly hack, may fail easily
				source.contains("node") || target.contains("node") ? MessageType.AGENT_CONTENT : MessageType.CONTENT,
				data);
		li("Send message [] from [] to [] []", content, source, target,
				outQueue != null ? "will queue" : "will not queue");
		if(outQueue != null) {
			outQueue.add(message);
			return true;
		}
		
		if(target.contains("Monitoring"))
			// FIXME: does this actually occur anymore?
			return true;
		if(wsClient != null)
			wsClient.send(message);
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			if(event.get(CompositeAgent.TRANSIENT_EVENT_PARAMETER) != null) {
				li("Agent started after move. Queued messages: [] in / [] out", inQueue.size(), outQueue.size());
				// String entityName = getAgent().getEntityName();
				// String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null,
				// String.valueOf(new Timestamp(System.currentTimeMillis())));
				// pylon.send(this.getName(), pylon.getEntityName(), notify_content);
				// pylon.register(entityName, inbox); // already done in AbstractMessagingShard
				startShadowAgentShard(MessageType.CONNECT);
				// System.out.println();
			}
			else {
				this.register(getAgent().getEntityName());
				startShadowAgentShard(MessageType.REGISTER);
			}
			getAgent().postAgentEvent(new AgentEvent(AgentEventType.AFTER_MOVE));
			break;
		case BEFORE_MOVE:
			// create queues, no messages sent / received beyond this point
			inQueue = new LinkedBlockingQueue<>();
			outQueue = new LinkedBlockingQueue<>();
			
			nextMoveTarget = event.get("TARGET");
			lf("Agent " + this.getName() + " wants to move to another node " + nextMoveTarget);
			// String notify_content = createMonitorNotification(ActionType.MOVE_TO_ANOTHER_NODE, null,
			// String.valueOf(new Timestamp(System.currentTimeMillis())));
			// pylon.send(this.getName(), event.get("pylon_destination"), notify_content);
			// pylon.unregister(getName(), inbox); // already done in AbstractMessagingShard
			wsClient.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.REQ_LEAVE, null));
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
	
	protected void messageTriggeredBehavior(String s) {
		Object obj = JSONValue.parse(s);
		if(obj == null)
			return;
		JSONObject message = (JSONObject) obj;
		String str = (String) message.get("type");
		String content;
		switch(MessageFactory.MessageType.valueOf(str)) {
		case CONTENT:
			content = createMonitorNotification(MessageFactory.ActionType.RECEIVE_MESSAGE,
					(String) message.get("content"), String.valueOf(new Timestamp(System.currentTimeMillis())));
			li("Message from []: [] []", message.get("source"), message.get("content"),
					inQueue != null ? "will queue" : "will not queue");
			inbox.receive((String) message.get("source"), (String) message.get("destination"), content);
			// pylon.send((String) message.get("source"), (String) message.get("destination"), content);
			break;
		case REQ_ACCEPT:
			wsClient.client.close();
			wsClient = null;
			li("Prepared to leave. Queued messages: [] in / [] out", inQueue.size(), outQueue.size());
			// content = createMonitorNotification(MessageFactory.ActionType.MOVE_TO_ANOTHER_NODE, null,
			// String.valueOf(new Timestamp(System.currentTimeMillis())));
			// inbox.receive(null, null, content);
			
			getAgent().postAgentEvent((AgentEvent) new AgentEvent(AgentEventType.AGENT_STOP)
					.add(CompositeAgent.TRANSIENT_EVENT_PARAMETER, MobileCompositeAgent.MOVE_TRANSIENT_EVENT_PARAMETER)
					.add(MobileCompositeAgent.TARGET, nextMoveTarget));
			nextMoveTarget = null;
		case AGENT_CONTENT:
			li("Received agent from " + message.get("source"));
			// content = createMonitorNotification(ActionType.RECEIVE_MESSAGE, (String) message.get("content"),
			// String.valueOf(new Timestamp(System.currentTimeMillis())));
			// pylon.send((String) message.get("source"), (String) message.get("destination"), content);
			AgentEvent arrived_agent = new AgentWave();
			arrived_agent.add("content", (String) message.get("content"));
			arrived_agent.add("destination-complete", (String) message.get("destination"));
			getAgent().postAgentEvent(arrived_agent);
			break;
		default:
			le("Unknown type");
		}
	}

	@Override
	protected void receiveMessage(String source, String destination, String content) {
		Object obj = JSONValue.parse(content);
		if(obj == null)
			return;
		JSONObject mesg = (JSONObject) obj;
		String type = (String) mesg.get("action");
		switch(MessageFactory.ActionType.valueOf(type)) {
		case RECEIVE_MESSAGE:
			String server = (String) mesg.get("server");
			if(server != null) {
				try {
					serverURI = new URI(server);
					li("After moving connect to " + serverURI);
				} catch(URISyntaxException e) {
					le("Incorrect URI format []", server);
				}
				break;
			}
			if(inQueue != null)
				inQueue.add(new AbstractMap.SimpleEntry<Map.Entry<String, String>, String>(
						new AbstractMap.SimpleEntry<>(source, destination), (String) mesg.get("content")));
			else
				super.receiveMessage(source, destination, (String) mesg.get("content"));
			// pylon.send(source, destination, content);
			// li("Check: [] in / [] out", inQueue.size(), outQueue.size());
			
			break;
		case MOVE_TO_ANOTHER_NODE:
		default:
			break;
		}
	}
}

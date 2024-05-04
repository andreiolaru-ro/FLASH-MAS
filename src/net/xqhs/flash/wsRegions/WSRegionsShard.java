package net.xqhs.flash.wsRegions;

import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobilityAwareMessagingShard;
import net.xqhs.flash.core.support.NameBasedMessagingShard;
import net.xqhs.flash.json.AgentWaveJson;
import net.xqhs.flash.wsRegions.Constants.MessageType;

public class WSRegionsShard extends NameBasedMessagingShard implements MobilityAwareMessagingShard {
	/**
	 * The serial UID.
	 */
	private static final long		serialVersionUID	= 9027349979793742065L;
	/**
	 * The URI of the Region server.
	 */
	// transient URI serverURI;
	transient String				nextMoveTarget;
	LinkedBlockingQueue<AgentWave>	inQueue;
	LinkedBlockingQueue<AgentWave>	outQueue;
	
	/**
	 * @param eventType
	 *            - can be one of {@link MessageType#REGISTER} or {@link MessageType#CONNECT} depending on whether the
	 *            entity registers for the first time or starts after a move.
	 */
	public void startShard(Constants.MessageType eventType) {
		setUnitName(getAgent().getEntityName());
		
		while(inQueue != null && !inQueue.isEmpty()) {
			AgentWave wave = inQueue.poll();
			if(inQueue.isEmpty())
				inQueue = null;
			lf("Managing queued message: []", wave);
			super.receiveWave(wave);
		}
		inQueue = null;
		sendProtocolMessage(eventType);
		while(outQueue != null && !outQueue.isEmpty())
			sendMessage(outQueue.poll());
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
	
	// @Override
	// public boolean sendMessage(String source, String target, String content) {
	// // String notify_content = createMonitorNotification(ActionType.SEND_MESSAGE, content,
	// // String.valueOf(new Timestamp(System.currentTimeMillis())));
	// // pylon.send(this.getName(), target, notify_content);
	//
	// // Map<String, String> data = new HashMap<>();
	// // data.put("destination", target);
	// // data.put("content", content);
	// // String message = MessageFactory.createMessage(classicPylon.getEntityName(), this.getName(),
	// // // FIXME: very ugly hack, may fail easily
	// // source.contains("node") || target.contains("node") ? Constants.MessageType.AGENT_CONTENT
	// // : Constants.MessageType.CONTENT,
	// // data);
	// // li("Send message [] from [] to [] []", content, source, target,
	// // outQueue != null ? "will queue" : "will not queue");
	// if(outQueue != null) {
	// outQueue.add(message);
	// return true;
	// }
	//
	// // if(target.contains("Monitoring"))
	// // // FIXME: does this actually occur anymore?
	// // return true;
	// if(wsClient != null)
	// wsClient.send(message);
	// return true;
	// }
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			if(event.get(CompositeAgent.TRANSIENT_EVENT_PARAMETER) != null) {
				li("Agent started after move. Queued messages: [] in / [] out", Integer.valueOf(inQueue.size()),
						Integer.valueOf(outQueue.size()));
				// String entityName = getAgent().getEntityName();
				// String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null,
				// String.valueOf(new Timestamp(System.currentTimeMillis())));
				// pylon.send(this.getName(), pylon.getEntityName(), notify_content);
				// pylon.register(entityName, inbox); // already done in AbstractMessagingShard
				startShard(Constants.MessageType.CONNECT);
				// System.out.println();
				getAgent().postAgentEvent(new AgentEvent(AgentEventType.AFTER_MOVE));
			}
			else {
				// shard initially booting.
				// register(getName());
				startShard(Constants.MessageType.REGISTER);
			}
			break;
		case BEFORE_MOVE:
			// create queues, no messages sent / received beyond this point
			inQueue = new LinkedBlockingQueue<>();
			outQueue = new LinkedBlockingQueue<>();
			
			nextMoveTarget = event.get(MobileCompositeAgent.TARGET);
			lf("Agent " + this.getName() + " wants to move to another node " + nextMoveTarget);
			// String notify_content = createMonitorNotification(ActionType.MOVE_TO_ANOTHER_NODE, null,
			// String.valueOf(new Timestamp(System.currentTimeMillis())));
			// pylon.send(this.getName(), event.get("pylon_destination"), notify_content);
			// pylon.unregister(getName(), inbox); // already done in AbstractMessagingShard
			sendProtocolMessage(MessageType.REQ_LEAVE);
			break;
		case AFTER_MOVE:
			// String entityName = getAgent().getEntityName();
			// String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null,
			// String.valueOf(new Timestamp(System.currentTimeMillis())));
			// pylon.send(this.getName(), pylon.getEntityName(), notify_content);
			
			// pylon.register(entityName, inbox);
			// wsClient.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONNECT, null));
			break;
		default:
			break;
		}
	}
	
	protected void sendProtocolMessage(MessageType event) {
		wavePylon.send((AgentWave) new AgentWaveJson().addSourceElements(getAgentAddress(), Constants.PROTOCOL)
				.add(Constants.EVENT_TYPE_KEY, event.toString()));
	}
	
	@Override
	protected void receiveWave(AgentWave wave) {
		dbg(Constants.Dbg.DEBUG_WSREGIONS, "received message []; in/out: []/[]", wave, inQueue, outQueue);
		String[] dest = wave.getDestinationElements();
		if(dest.length >= 2 && dest[0].equals(getAgentAddress()) && dest[1].equals(Constants.PROTOCOL))
			switch(MessageType.valueOf(wave.get(Constants.EVENT_TYPE_KEY))) {
			case REQ_ACCEPT:
				li("Prepared to leave. Queued messages: [] in / [] out", Integer.valueOf(inQueue.size()),
						Integer.valueOf(outQueue.size()));
				// content = createMonitorNotification(MessageFactory.ActionType.MOVE_TO_ANOTHER_NODE, null,
				// String.valueOf(new Timestamp(System.currentTimeMillis())));
				// inbox.receive(null, null, content);
				
				getAgent().postAgentEvent((AgentEvent) new AgentEvent(AgentEventType.AGENT_STOP)
						.add(CompositeAgent.TRANSIENT_EVENT_PARAMETER,
								MobileCompositeAgent.MOVE_TRANSIENT_EVENT_PARAMETER)
						.add(MobileCompositeAgent.TARGET, nextMoveTarget));
				nextMoveTarget = null;
				break;
			case AGENT_CONTENT:
				li("Received agent from ", wave.getCompleteSource());
				// content = createMonitorNotification(ActionType.RECEIVE_MESSAGE, (String) message.get("content"),
				// String.valueOf(new Timestamp(System.currentTimeMillis())));
				// pylon.send((String) message.get("source"), (String) message.get("destination"), content);
				getAgent().postAgentEvent(wave);
				break;
			default:
				le("Received protocol message of unusable type: ", wave);
				break;
			}
		else if(inQueue != null)
			inQueue.add(wave);
		else
			super.receiveWave(wave);
	}
	
	// protected void processMessage(String s) {
	// Object obj = JSONValue.parse(s);
	// if(obj == null)
	// return;
	// JSONObject message = (JSONObject) obj;
	// String str = (String) message.get("type");
	// String content;
	// switch(MessageFactory.MessageType.valueOf(str)) {
	// case CONTENT:
	// content = MessageFactory.createMonitorNotification(MessageFactory.ActionType.RECEIVE_MESSAGE,
	// (String) message.get("content"), String.valueOf(new Timestamp(System.currentTimeMillis())));
	// li("Message from []: [] []", message.get("source"), message.get("content"),
	// inQueue != null ? "will queue" : "will not queue");
	// classicInbox.receive((String) message.get("source"), (String) message.get("destination"), content);
	// // pylon.send((String) message.get("source"), (String) message.get("destination"), content);
	// break;
	//
	// }
	// }
	
	// @Override
	// protected void receiveMessage(String source, String destination, String content) {
	// Object obj = JSONValue.parse(content);
	// if(obj == null)
	// return;
	// JSONObject mesg = (JSONObject) obj;
	// String type = (String) mesg.get("action");
	// switch(MessageFactory.ActionType.valueOf(type)) {
	// case RECEIVE_MESSAGE:
	// String server = (String) mesg.get("server");
	// if(server != null) {
	// try {
	// serverURI = new URI(server);
	// li("After moving connect to " + serverURI);
	// } catch(URISyntaxException e) {
	// le("Incorrect URI format []", server);
	// }
	// break;
	// }
	// if(inQueue != null)
	// inQueue.add(new AbstractMap.SimpleEntry<Map.Entry<String, String>, String>(
	// new AbstractMap.SimpleEntry<>(source, destination), (String) mesg.get("content")));
	// else
	// super.receiveMessage(source, destination, (String) mesg.get("content"));
	// // pylon.send(source, destination, content);
	// // li("Check: [] in / [] out", inQueue.size(), outQueue.size());
	//
	// break;
	// case MOVE_TO_ANOTHER_NODE:
	// default:
	// break;
	// }
	// }
}

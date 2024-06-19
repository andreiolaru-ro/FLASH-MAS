package net.xqhs.flash.wsRegions;

import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.composite.CompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent;
import net.xqhs.flash.core.mobileComposite.MobilityAwareMessagingShard;
import net.xqhs.flash.core.support.URIBasedMessagingShard;
import net.xqhs.flash.json.AgentWaveJson;
import net.xqhs.flash.wsRegions.Constants.MessageType;

/**
 * Manages messaging using the WS Regions protocol.
 * 
 * @author Andrei Olaru
 * @author Monica Pricope
 */
public class WSRegionsShard extends URIBasedMessagingShard implements MobilityAwareMessagingShard {
	/**
	 * The serial UID.
	 */
	private static final long		serialVersionUID	= 9027349979793742065L;
	/**
	 * Stores the target of the moving agent, while the move is accepted and the agent closes.
	 */
	transient String				nextMoveTarget;
	/**
	 * Between triggering a move and actually stopping the agent, this stores any received messages, that will be posted
	 * on the event queue after the agent arrives.
	 */
	LinkedBlockingQueue<AgentWave>	inQueue;
	/**
	 * Between triggering a move and actually stopping the agent, this stores any outgoing messages, that will be send
	 * after the agent arrives..
	 */
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
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
		case AGENT_START:
			if(event.get(CompositeAgent.TRANSIENT_EVENT_PARAMETER) != null) {
				li("Agent started after move. Queued messages: [] in / [] out", Integer.valueOf(inQueue.size()),
						Integer.valueOf(outQueue.size()));
				startShard(Constants.MessageType.CONNECT);
				getAgent().postAgentEvent(new AgentEvent(AgentEventType.AFTER_MOVE));
			}
			else {
				// shard initially booting.
				startShard(Constants.MessageType.REGISTER);
			}
			break;
		case BEFORE_MOVE:
			// create queues, no messages sent / received beyond this point
			inQueue = new LinkedBlockingQueue<>();
			outQueue = new LinkedBlockingQueue<>();
			
			nextMoveTarget = event.get(MobileCompositeAgent.TARGET);
			lf("Agent " + this.getName() + " wants to move to another node " + nextMoveTarget);
			sendProtocolMessage(MessageType.REQ_LEAVE);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Sends a protocol-specific message to the current region server.
	 * 
	 * @param event
	 *            - the {@link MessageType} to convey to the server.
	 */
	protected void sendProtocolMessage(MessageType event) {
		wavePylon.send((AgentWave) new AgentWaveJson().addSourceElements(getAgentAddress(), Constants.PROTOCOL)
				.add(Constants.EVENT_TYPE_KEY, event.toString()));
	}
	
	/**
	 * Intercepts protocol-specific messages from the server, and, if the agent is going to move, queues any incoming
	 * messages in the {@link #inQueue}.
	 */
	@Override
	protected void receiveWave(AgentWave wave) {
		dbg(Constants.Dbg.DEBUG_WSREGIONS, "received message []; in/out: []/[]", wave, inQueue, outQueue);
		String[] dest = wave.getDestinationElements();
		if(dest.length >= 2 && dest[0].equals(getAgentAddress()) && dest[1].equals(Constants.PROTOCOL))
			switch(MessageType.valueOf(wave.get(Constants.EVENT_TYPE_KEY))) {
			case REQ_ACCEPT:
				li("Prepared to leave. Queued messages: [] in / [] out", Integer.valueOf(inQueue.size()),
						Integer.valueOf(outQueue.size()));
				
				getAgent().postAgentEvent((AgentEvent) new AgentEvent(AgentEventType.AGENT_STOP)
						.add(CompositeAgent.TRANSIENT_EVENT_PARAMETER,
								MobileCompositeAgent.MOVE_TRANSIENT_EVENT_PARAMETER)
						.add(MobileCompositeAgent.TARGET, nextMoveTarget));
				nextMoveTarget = null;
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
}

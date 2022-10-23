package wsRegions;

import static wsRegions.MessageFactory.createMessage;
import static wsRegions.MessageFactory.createMonitorNotification;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.mobileComposite.MobileCompositeAgent;
import net.xqhs.flash.core.mobileComposite.NonSerializableShard;
import net.xqhs.flash.core.support.AbstractNameBasedMessagingShard;
import net.xqhs.flash.core.support.MessageReceiver;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.util.PlatformUtils;
import wsRegions.MessageFactory.ActionType;
import wsRegions.MessageFactory.MessageType;

public class WSRegionsShard extends AbstractNameBasedMessagingShard implements NonSerializableShard {
	/**
	 * Reference to the local pylon proxy
	 */
	private MessagingPylonProxy pylon = null;
	
	/**
	 * The {@link MessageReceiver} instance of this shard.
	 */
	public MessageReceiver	inbox;
	/**
	 * the Websocket object connected to Region server.
	 */
	protected WSClient		client;
	URI						serverURI;
	
	/**
	 * No-argument constructor
	 */
	public WSRegionsShard() {
		inbox = new MessageReceiver() {
			@Override
			public void receive(String source, String destination, String content) {
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
					receiveMessage(source, destination, (String) mesg.get("content"));
					// pylon.send(source, destination, content);
					break;
				case MOVE_TO_ANOTHER_NODE:
				default:
					break;
				}
			}
		};
	}
	
	public void startShadowAgentShard(MessageType connection_type) {
		setUnitName(getAgent().getEntityName());
		setLoggerType(PlatformUtils.platformLogType());
		client = new WSClient(serverURI, 10, 5000, this.getLogger()) {
			@Override
			public void onMessage(String s) {
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
					inbox.receive((String) message.get("source"), (String) message.get("destination"), content);
					pylon.send((String) message.get("source"), (String) message.get("destination"), content);
					li("Message from " + message.get("source") + ": " + message.get("content"));
					break;
				case REQ_ACCEPT:
					li("[] Prepare to leave", getUnitName());
					content = createMonitorNotification(MessageFactory.ActionType.MOVE_TO_ANOTHER_NODE, null,
							String.valueOf(new Timestamp(System.currentTimeMillis())));
					inbox.receive(null, null, content);
					client.close();
					break;
				case AGENT_CONTENT:
					li("Received agent from " + message.get("source"));
					content = createMonitorNotification(ActionType.RECEIVE_MESSAGE, (String) message.get("content"),
							String.valueOf(new Timestamp(System.currentTimeMillis())));
					pylon.send((String) message.get("source"), (String) message.get("destination"), content);
					AgentEvent arrived_agent = new AgentWave();
					arrived_agent.add("content", (String) message.get("content"));
					arrived_agent.add("destination-complete", (String) message.get("destination"));
					getAgent().postAgentEvent(arrived_agent);
					break;
				default:
					le("Unknown type");
				}
			}
		};
		client.send(createMessage(pylon.getEntityName(), this.getName(), connection_type, null));
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
	public boolean sendMessage(String source, String target, String content) {
		li("Send message <<" + content + ">> from agent " + source + " to agent " + target);
		String notify_content = createMonitorNotification(ActionType.SEND_MESSAGE, content,
				String.valueOf(new Timestamp(System.currentTimeMillis())));
		pylon.send(this.getName(), target, notify_content);
		
		Map<String, String> data = new HashMap<>();
		data.put("destination", target);
		data.put("content", content);
		if(client != null) {
			if(target.contains("Monitoring")) {
				return true;
			}
			if(source.contains("node") || target.contains("node")) {
				client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.AGENT_CONTENT, data));
			}
			else {
				client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONTENT, data));
			}
		}
		return true;
	}
	
	@Override
	protected void receiveMessage(String source, String destination, String content) {
		super.receiveMessage(source, destination, content);
	}
	
	@Override
	public void register(String entityName) {
		pylon.register(entityName, inbox);
		// client.send(createMessage(pylon.getEntityName(), this.getName(), MessageFactory.MessageType.REGISTER, null));
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!(context instanceof MessagingPylonProxy))
			return false;
		pylon = (MessagingPylonProxy) context;
		return true;
	}
	
	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		if(event.getType().equals(AgentEvent.AgentEventType.AGENT_START)) {
			if(event.get(MobileCompositeAgent.TRANSIENT_EVENT_PARAMETER) != null) {
				li("Agent started after move.");
				String entityName = getAgent().getEntityName();
				String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null,
						String.valueOf(new Timestamp(System.currentTimeMillis())));
				pylon.send(this.getName(), pylon.getEntityName(), notify_content);
				pylon.register(entityName, inbox);
				startShadowAgentShard(MessageType.CONNECT);
				System.out.println();
			}
			else {
				this.register(getAgent().getEntityName());
				startShadowAgentShard(MessageType.REGISTER);
			}
			
		}
		
		if(event.getType().equals(AgentEvent.AgentEventType.AGENT_STOP)) {
			String target = event.get("TARGET");
			lf("Agent " + this.getName() + " wants to move to another node " + target);
			String notify_content = createMonitorNotification(ActionType.MOVE_TO_ANOTHER_NODE, null,
					String.valueOf(new Timestamp(System.currentTimeMillis())));
			pylon.send(this.getName(), event.get("pylon_destination"), notify_content);
			client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.REQ_LEAVE, null));
		}
		
		if(event.getType().equals(AgentEvent.AgentEventType.AFTER_MOVE)) {
			String entityName = getAgent().getEntityName();
			String notify_content = createMonitorNotification(ActionType.ARRIVED_ON_NODE, null,
					String.valueOf(new Timestamp(System.currentTimeMillis())));
			pylon.send(this.getName(), pylon.getEntityName(), notify_content);
			
			pylon.register(entityName, inbox);
			client.send(createMessage(pylon.getEntityName(), this.getName(), MessageType.CONNECT, null));
		}
	}
	
	@Override
	public String getName() {
		return getAgent().getEntityName();
	}
	
	@Override
	public String getAgentAddress() {
		return this.getName();
	}
}

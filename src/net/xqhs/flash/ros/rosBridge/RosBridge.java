package net.xqhs.flash.ros.rosBridge;


import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest;
import org.eclipse.jetty.websocket.client.WebSocketClient;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *
 * A socket for connecting to ros bridge that accepts subscribe and publish commands. Subscribing to a topic using the
 * {@link #subscribe(SubscriptionRequestMsg, RosListenDelegate)}. The input {@link SubscriptionRequestMsg} allows you to
 * iteratively build all the optional fields you can set to detail the subscription, such as whether the messages should
 * be fragmented in size, the queue length, throttle rate, etc. If data is pushed quickly, it is recommended that you
 * set the throttle rate and queue length to 1 or you may observe increasing latency in the messages. Png compression is
 * currently not supported. If the message type is not set, and the topic either does not exist or you have never
 * subscribed to that topic previously, Rosbridge may fail to subscribe. There are also additional methods for
 * subscribing that take the parameters of a subscription as arguments to the method.
 * <p>
 * Publishing is also supported with the {@link #publish(String, String, Object)} method, but you should consider using
 * the {@link net.xqhs.flash.ros.rosBridge.Publisher} class wrapper for streamlining publishing.
 * <p>
 * To create and connect to rosbridge, you can either instantiate with the default constructor and then call
 * {connect(String)} or use the static method {createConnection(String)} which creates a RosBridge instance and then
 * connects. An example URI to provide as a parameter is: ws://localhost:9090, where 9090 is the default Rosbridge
 * server port.
 * <p>
 * If you need to handle messages with larger sizes, you should subclass RosBridge and annotate the class with
 * {@link WebSocket} with the parameter maxTextMessageSize set to the desired buffer size. For example:
 * <p>
 * <code>
 *	{@literal @}WebSocket(maxTextMessageSize = 500 * 1024)
 *	public class BigRosBridge extends RosBridge{}
 * </code>
 * <p>
 * Note that the subclass does not need to override any methods; subclassing is performed purely to set the buffer size
 * in the annotation value. Then you can instantiate BigRosBridge and call its inherited connect method.
 *
 * @author James MacGlashan.
 */
@WebSocket
public class RosBridge {

	protected final CountDownLatch closeLatch;
	protected Session session;

	protected Map<String, RosBridgeSubscriber> listeners = new ConcurrentHashMap<String, RosBridgeSubscriber>();

	protected boolean hasConnected = false;

	protected boolean printMessagesAsReceived = false;

	/**
	 * Connects to the Rosbridge host at the provided URI.
	 * @param rosBridgeURI the URI to the ROS Bridge websocket server. Note that ROS Bridge by default uses port 9090. An example URI is: ws://localhost:9090
	 * @param waitForConnection if true, then this method will block until the connection is established. If false, then return immediately.
	 */
	public void connect(String rosBridgeURI, boolean waitForConnection){
		WebSocketClient client = new WebSocketClient();
		try {
			client.start();
			URI echoUri = new URI(rosBridgeURI);
			ClientUpgradeRequest request = new ClientUpgradeRequest();
			client.connect(this, echoUri, request);
			System.out.printf("Connecting to : %s%n", echoUri);
			if(waitForConnection){
				this.waitForConnection();
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

	}

	public RosBridge(){
		this.closeLatch = new CountDownLatch(1);
	}


	/**
	 * Blocks execution until a connection to the ros bridge server is established.
	 */
	public void waitForConnection(){

		if(this.hasConnected){
			return; //done
		}

		synchronized(this){
			while(!this.hasConnected){
				try {
					this.wait();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.printf("Got connect for ros: %s%n", session);
		this.session = session;
		this.hasConnected = true;
		synchronized(this) {
			this.notifyAll();
		}

	}

	@OnWebSocketMessage
	public void onMessage(String msg) {

		if(this.printMessagesAsReceived){
			System.out.println(msg);
		}

		ObjectMapper mapper = new ObjectMapper();
		JsonNode node = null;
		try {
			node = mapper.readTree(msg);
			if(node.has("op")){
				String op = node.get("op").asText();
				if(op.equals("publish")){
					String topic = node.get("topic").asText();
					RosBridgeSubscriber subscriber = this.listeners.get(topic);
					if(subscriber != null){
						subscriber.receive(node, msg);
					}
				}
			}
		} catch(IOException e) {
			System.out.println("Could not parse ROSBridge web socket message into JSON data");
			e.printStackTrace();
		}

	}


	/**
	 * Subscribes to a topic with the subscription parameters specified in the provided {@link SubscriptionRequestMsg}.
	 * The {@link RosListenDelegate} will be notified every time there is a publish to the specified topic.
	 * @param request the subscription request details.
	 * @param delegate the delegate that will receive messages each time a message is published to the topic.
	 */
	public void subscribe(SubscriptionRequestMsg request, RosListenDelegate delegate){

		if(this.session == null){
			throw new RuntimeException("Rosbridge connection is closed. Cannot subscribe.");
		}

		String topic = request.getTopic();

		//already have a subscription? just update delegate
		synchronized(this.listeners){
			RosBridgeSubscriber subscriber = this.listeners.get(topic);
			if(subscriber!=null){
				subscriber.addDelegate(delegate);
				return;
			}

			//otherwise setup the subscription and delegate
			this.listeners.put(topic, new RosBridgeSubscriber(delegate));
		}

		String subMsg = request.generateJsonString();
		Future<Void> fut;
		try{
			fut = session.getRemote().sendStringByFuture(subMsg);
			fut.get(2, TimeUnit.SECONDS);
		}catch (Throwable t){
			System.out.println("Error in sending subscription message to Rosbridge host for topic " + topic);
			t.printStackTrace();
		}


	}


	/**
	 * Publishes to a topic. If the topic has not already been advertised on ros, it will automatically do so.
	 * @param topic the topic to publish to
	 * @param type the message type of the topic
	 * @param msg should be a {@link Map} or a Java Bean, specifying the ROS message
	 */
	public void publish(String topic, String type, Object msg){

		if(this.session == null){
			throw new RuntimeException("Rosbridge connection is closed. Cannot publish. Attempted Topic Publish: " + topic);
		}

		Map<String, Object> jsonMsg = new HashMap<String, Object>();
		jsonMsg.put("op", "publish");
		jsonMsg.put("topic", topic);
		jsonMsg.put("type", type);
		jsonMsg.put("msg", msg);

		JsonFactory jsonFactory = new JsonFactory();
		StringWriter writer = new StringWriter();
		JsonGenerator jsonGenerator;
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			jsonGenerator = jsonFactory.createGenerator(writer);
			objectMapper.writeValue(jsonGenerator, jsonMsg);
		} catch(Exception e){
			System.out.println("Error");
		}

		String jsonMsgString = writer.toString();
		Future<Void> fut;
		try{
			fut = session.getRemote().sendStringByFuture(jsonMsgString);
			fut.get(2, TimeUnit.SECONDS);
		}catch (Throwable t){
			System.out.println("Error publishing to " + topic + " with message type: " + type);
			t.printStackTrace();
		}

	}


	/**
	 * Class for managing all the listeners that have subscribed to a topic on Rosbridge.
	 * Maintains a list of {@link RosListenDelegate} objects and informs them all
	 * when a message has been received from Rosbridge.
	 */
	public static class RosBridgeSubscriber{

		protected List<RosListenDelegate> delegates = new CopyOnWriteArrayList<RosListenDelegate>();


		/**
		 * Initializes and adds all the input delegates to receive messages.
		 * @param delegates the delegates to receive messages.
		 */
		public RosBridgeSubscriber(RosListenDelegate...delegates) {
			for(RosListenDelegate delegate : delegates){
				this.delegates.add(delegate);
			}
		}

		/**
		 * Adds a delegate to receive messages from Rosbridge.
		 * @param delegate a delegate to receive messages from Rosbridge.
		 */
		public void addDelegate(RosListenDelegate delegate){
			this.delegates.add(delegate);
		}


		/**
		 * Receives a new published message to a subscribed topic and informs all listeners.
		 * @param data the {@link com.fasterxml.jackson.databind.JsonNode} containing the JSON data received.
		 * @param stringRep the string representation of the JSON object.
		 */
		public void receive(JsonNode data, String stringRep){
			for(RosListenDelegate delegate : delegates){
				delegate.receive(data, stringRep);
			}
		}

	}

}

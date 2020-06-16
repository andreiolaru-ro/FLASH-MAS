package net.xqhs.flash.ros.rosBridge;

/**
 * A wrapper class for streamlining ROS Topic publishing. Note that the {@link #advertise()} never *needs* to be explicitly
 * called. If you use the standard {@link #Publisher(String, String, RosBridge)} method, it will be automatically
 * called on construction and if you use the {@link #Publisher(String, String, RosBridge, boolean)} method
 * and set the advertiseNow flag to false, you still don't *need* to call it, because the first publish will
 * automatically make sure the topic has been advertised first.
 * <p>
 * Publish messages using the {@link #publish(Object)} method. It takes an object containing the ROS message
 * to publish. Generally, the msg should either be a Javabean, such as one of the pre-included
 * messages in the {@link msgs} package that has the same field structure as the target topic type
 * or a {@link java.util.Map} object
 * representing the ROS message type structure. For
 * example, if the ROS message type is "std_msgs/String" then msg should be a {@link msgs.std_msgs.PrimitiveMsg}
 * with the generic of String, or a Map object
 * with one Map key-value entry of "data: stringValue" where stringValue is whatever the "std_msgs/String"
 * data field value is.
 * @author James MacGlashan.
 */
public class Publisher {

	protected String topic;
	protected String msgType;
	protected RosBridge rosBridge;


	/**
	 * Constructs and automatically advertises publishing to this topic.
	 * @param topic the topic to which messages will be published
	 * @param msgType the ROS message type of the topic
	 * @param rosBridge the {@link rosBridge.RosBridge} that manages ROS Bridge interactions.
	 */
	public Publisher(String topic, String msgType, RosBridge rosBridge){
		this.topic = topic;
		this.msgType = msgType;
		this.rosBridge = rosBridge;

	}


	/**
	 * Publishes the message. If this client is not already advertising for this topic, it automatically will first.<p>
	 * Generally, the msg should either be a Javabean, such as one of the pre-included
	 * messages in the {@link ros.msgs} package that has the same field structure as the target topic type
	 * or a {@link java.util.Map} object
	 * representing the ROS message type structure. For
	 * example, if the ROS message type is "std_msgs/String" then msg should be a {@link ros.msgs.std_msgs.PrimitiveMsg}
	 * with the generic of String, or a Map object
	 * with one Map key-value entry of "data: stringValue" where stringValue is whatever the "std_msgs/String"
	 * data field value is.
	 * @param msg the message to publish.
	 */
	public void publish(Object msg){
		this.rosBridge.publish(this.topic, this.msgType, msg);
	}

}

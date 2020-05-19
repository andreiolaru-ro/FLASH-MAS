package net.xqhs.flash.ros.rosBridge.msgs.std_msgs;

/**
 * @author James MacGlashan.
 */
public class Time {
	public int secs;
	public int nsecs;

	public Time() {
	}

	public Time(int secs, int nsecs) {
		this.secs = secs;
		this.nsecs = nsecs;
	}
}

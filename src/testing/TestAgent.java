package testing;

import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.agent.BaseAgent;
import net.xqhs.flash.core.util.MultiTreeMap;

/**
 * Non-communicating agent that prints a log line for a few times and then stops.
 */
public class TestAgent extends BaseAgent {
	/**
	 * The serial UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The timer for logs.
	 */
	protected Timer				timer				= new Timer();
	/**
	 * Current task.
	 */
	private int					index;
	
	@Override
	public boolean configure(MultiTreeMap configuration) {
		if(!super.configure(configuration))
			return false;
		return true;
	}
	
	@Override
	public boolean start() {
		super.start();
		
		li("Agent started");
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				li("Task", Integer.valueOf(getIndex()));
				setIndex(getIndex() + 1);
			}
		}, 1000, 1000);
		
		return true;
	}
	
	@Override
	public boolean stop() {
		timer.cancel();
		return super.stop();
	}
	
	@Override
	protected void li(String message, Object... arguments) {
		super.li(message, arguments);
	}
	
	/**
	 * @return the index
	 */
	public int getIndex() {
		return index;
	}
	
	/**
	 * @param index
	 *            - index to set.
	 */
	public void setIndex(int index) {
		this.index = index;
		if(index > 5)
			stop();
	}
	
}

package testing;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.Entity.EntityProxy;
import net.xqhs.flash.core.node.Node;
import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.Unit;

/**
 * Centralizes (on a node) the monitoring of events, with time stamps. The events are sent by TODO
 * <p>
 * All the events are recorded in a YAML file, asynchronously.
 * 
 * @author Andrei Olaru
 */
public class TimeMonitor extends Unit implements Entity<Node>, EntityProxy<TimeMonitor> {
	/**
	 * The thread that manages the message queue.
	 * 
	 * @author Andrei Olaru
	 */
	class MessageThread implements Runnable {
		@Override
		public void run() {
			processQueue();
		}
	}
	
	protected static int											CHUNK_SIZE			= 10;
	protected static int											TIME_STEP			= 500;
	protected String												nodeName;
	private static FileWriter										writer;
	protected static Logger											log;
	protected static boolean										useThread			= true;
	protected static Thread											processingThread	= null;
	/**
	 * If a separate thread is used for messages ({@link #useThread} is <code>true</code>) this queue is used to gather
	 * print items.
	 */
	protected static LinkedBlockingQueue<Map.Entry<Long, Object[]>>	processingQueue		= null;
	
	public TimeMonitor() {
		setUnitName("time");
	}
	
	@Override
	public boolean start() {
		log = getLogger();
		try {
			String filename = "log-" + (nodeName != null ? nodeName : "monitor") + ".yaml";
			writer = new FileWriter(filename, false);
		} catch(IOException e) {
			le("An error occurred.");
			e.printStackTrace();
			return false;
		}
		if(useThread) {
			processingQueue = new LinkedBlockingQueue<>();
			processingThread = new Thread(new MessageThread());
			processingThread.start();
		}
		return true;
	}
	
	@Override
	public boolean stop() {
		if(writer != null)
			try {
				writer.close();
			} catch(IOException e) {
				le("An error occurred.");
				e.printStackTrace();
			}
		if(useThread) {
			useThread = false;
			synchronized(processingQueue) {
				processingQueue.clear();
				processingQueue.notifyAll();
			}
			if(processingThread != null)
				try {
					processingThread.join();
				} catch(InterruptedException e) {
					e.printStackTrace();
				}
			processingQueue = null;
			processingThread = null;
		}
		doExit();
		return true;
	}
	
	protected static void processQueue() {
		while(useThread) {
			// lf("Messages: ", Integer.valueOf(messageQueue.size()));
			try {
				Thread.sleep(TIME_STEP);
			} catch(InterruptedException e1) {
				e1.printStackTrace();
			}
			if(processingQueue.isEmpty())
				try {
					synchronized(processingQueue) {
						processingQueue.wait();
					}
				} catch(InterruptedException e) {
					// do nothing
				}
			else {
				writeChunk();
			}
		}
	}
	
	@Override
	public boolean isRunning() {
		return false;
	}
	
	@Override
	public String getName() {
		return null;
	}
	
	@Override
	public boolean addContext(EntityProxy<Node> context) {
		if(context != null)
			nodeName = context.getEntityName();
		return true;
	}
	
	@Override
	public boolean removeContext(EntityProxy<Node> context) {
		if(context != null && context.getEntityName().equals(nodeName))
			nodeName = null;
		return true;
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		
		return true;
	}
	
	@Override
	public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public EntityProxy<TimeMonitor> asContext() {
		return this;
	}
	
	@Override
	public String getEntityName() {
		return null;
	}
	
	public static boolean markTime(Object... prints) {
		if(writer == null)
			return false;
		long time = System.currentTimeMillis();
		if(useThread) {
			synchronized(processingQueue) {
				processingQueue.add(new AbstractMap.SimpleEntry<>(Long.valueOf(time), prints));
				processingQueue.notify();
			}
			return true;
		}
		return processEntry(time, prints);
	}
	
	protected static void writeChunk() {
		int nlines = processingQueue.size();
		if(nlines > CHUNK_SIZE && nlines < 3 * CHUNK_SIZE)
			nlines = CHUNK_SIZE;
		while(nlines > 0) {
			Map.Entry<Long, Object[]> entry = processingQueue.poll();
			if(entry != null)
				processEntry(entry.getKey().longValue(), entry.getValue());
			nlines--;
		}
		try {
			writer.flush();
		} catch(IOException e) {
			if(log != null)
				log.le("Failed to flush log ", e.getStackTrace().toString());
			else
				e.printStackTrace();
		}
	}
	
	protected static boolean processEntry(long time, Object... prints) {
		String line = time + " : [";
		line += "\"" + new Timestamp(time) + "\", ";
		line += String.join(", ",
				Arrays.asList(prints).stream().map(obj -> obj != null ? "\"" + obj.toString() + "\"" : "-")
						.collect(Collectors.toList()));
		line += "]\n";
		try {
			writer.write(line);
			if(!useThread)
				writer.flush();
			return true;
		} catch(IOException e) {
			if(log != null)
				log.le("Failed to write to log ", e.getStackTrace().toString());
			else
				e.printStackTrace();
			return false;
		}
	}
}

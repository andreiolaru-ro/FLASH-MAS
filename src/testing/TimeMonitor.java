package testing;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
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
	
	protected String			nodeName;
	private static FileWriter	writer;
	protected static Logger		log;
	
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
		doExit();
		return true;
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
		String line = "{" + time + " : [";
		line += "\"" + new Timestamp(time) + "\", ";
		line += String.join(", ",
				Arrays.asList(prints).stream().map(obj -> "\"" + obj.toString() + "\"").collect(Collectors.toList()));
		line += "]}\n";
		try {
			writer.write(line);
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

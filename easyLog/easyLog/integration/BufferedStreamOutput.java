package easyLog.integration;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.LinkedBlockingQueue;

import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.Logger.Level;
import net.xqhs.util.logging.output.StringLogOutput;

public class BufferedStreamOutput implements StringLogOutput {
	
	protected LinkedBlockingQueue<String>	messageQueue	= new LinkedBlockingQueue<>();
	boolean									running			= true;
	protected Thread						bufferThread;
	
	public BufferedStreamOutput(OutputStream destinationStream) {
		bufferThread = new Thread() {
			@Override
			public void run() {
				while(running) {
					if(messageQueue.isEmpty())
						try {
							synchronized(messageQueue) {
								messageQueue.wait(1000);
							}
						} catch(InterruptedException e) {
							// do nothing
						}
					else {
						String line = messageQueue.poll();
						try {
							destinationStream.write(line.getBytes());
						} catch(IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
		};
		bufferThread.start();
	}
	
	@Override
	public void update(String update) {
		synchronized(messageQueue) {
			messageQueue.add(update);
			messageQueue.notify();
		}
	}
	
	@Override
	public long getUpdatePeriod() {
		return 0;
	}
	
	@Override
	public int formatData() {
		return Logger.INCLUDE_NAME;
	}
	
	@Override
	public boolean useCustomFormat() {
		return false;
	}
	
	@Override
	public String format(Level level, String source, String message) {
		return null;
	}
	
	@Override
	public boolean updateWithEntireLog() {
		return false;
	}
	
	/**
	 * Stop the buffer thread.
	 */
	public void exit() {
		running = false;
	}
}

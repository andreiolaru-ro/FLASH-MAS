package net.xqhs.flash.shadowProtocol;

import java.net.URI;
import java.util.Arrays;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.LoggerClassic;

public abstract class WSClient extends WebSocketClient {
	
	Logger					log;
	URI						server;
	protected final String	logPre	= "<WSClient>";
	
	public WSClient(URI serverURI, int nTries, int deadline, LoggerClassic logger) {
		super(serverURI);
		log = logger;
		server = serverURI;
		long space = deadline / nTries;
		
		connect();
		
		int tries = nTries;
		while(tries-- > 0 && !isOpen()) {
			log.lf("<WSClient> Connection to [] is []. Tries left []", server, getReadyState(),
					Integer.valueOf(tries));
			try {
				Thread.sleep(space);
			} catch(InterruptedException e) {
				// just try again
			}
		}
	}
	
	public void onOpen(ServerHandshake arg0) {
		log.li("<WSClient> Connection to [] succeeded.", server);
	}

	public void onClose(int i, String s, boolean b) {
		log.lw("Closed with exit code " + i);
		log.lw("<WSClient> Connection to [] closed with code [].", server, i);
		
	}
	
	public void onError(Exception e) {
		log.le("<WSClient> Connection to [] erred:", Arrays.toString(e.getStackTrace()));
	}
	
	public abstract void onMessage(String message);
}

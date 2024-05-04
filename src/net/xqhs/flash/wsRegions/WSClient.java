package net.xqhs.flash.wsRegions;

import java.net.URI;
import java.util.Arrays;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.LoggerClassic;

/**
 * A wrapper of {@link WebSocketClient}, dealing with the process of repeated connection attempts.
 * 
 * @author Monica Pricope
 */
public abstract class WSClient {
	/**
	 * The state of the connection process.
	 */
	public enum ConnectionState {
		/**
		 * Initial connection is attempted.
		 */
		CONNECTING_INITIAL,
		
		/**
		 * Connection succeeded.
		 */
		CONNECTED,
		
		/**
		 * The initial process of connection failed.
		 */
		INITIAL_CONNECTION_FAILED
	}
	
	/**
	 * The log to use.
	 */
	Logger					log;
	/**
	 * The server URI.
	 */
	URI						server;
	/**
	 * Space between tries to connect.
	 */
	long					space;
	/**
	 * The state of the connection.
	 */
	ConnectionState			state;
	/**
	 * The actual {@link WebSocketClient} instance.
	 */
	WebSocketClient			client;
	/**
	 * Prefix for logging messages.
	 */
	protected final String	logPre	= "<WSClient>";
	
	/**
	 * Repeatedly attempts to create a WebSocket connection.
	 * 
	 * @param serverURI
	 *            - the URI.
	 * @param nTries
	 *            - how many times to attempt connection.
	 * @param deadline
	 *            - how long is allowed to spend on the connection process.
	 * @param logger
	 *            - the log to use.
	 */
	public WSClient(URI serverURI, int nTries, int deadline, LoggerClassic logger) {
		log = logger;
		server = serverURI;
		space = deadline / nTries;
		state = ConnectionState.CONNECTING_INITIAL;
		createClient();
		
		int tries = nTries;
		while(tries-- > 0 && !client.isOpen()) {
			log.lf("<WSClient> Connection to [] is []. Tries left []", server, client.getReadyState(),
					Integer.valueOf(tries));
			try {
				Thread.sleep(space);
			} catch(InterruptedException e) {
				// just try again
			}
		}
		if(client.isOpen())
			log.li("<WSClient> Connected successfully to [].", server);
		else {
			// client.close();
			state = ConnectionState.INITIAL_CONNECTION_FAILED;
			log.le("<WSClient> Connection to [] FAILED.", server);
		}
	}
	
	/**
	 * Creates the client.
	 */
	protected void createClient() {
		client = new WebSocketClient(server) {
			
			@Override
			public void onOpen(ServerHandshake arg0) {
				log.li("<WSClient> Connection to [] succeeded.", server);
			}
			
			@Override
			public void onMessage(String arg0) {
				WSClient.this.onMessage(arg0);
			}
			
			@Override
			public void onError(Exception e) {
				log.le("<WSClient> Connection to [] erred:", server, Arrays.toString(e.getStackTrace()));
				if(state == ConnectionState.CONNECTING_INITIAL) {
					try {
						Thread.sleep(space);
					} catch(InterruptedException e1) {
						// just try again
					}
					createClient();
				}
			}
			
			@Override
			public void onClose(int i, String s, boolean b) {
				log.lw("Closed with exit code " + i);
				log.lw("<WSClient> Connection to [] closed with code [].", server, Integer.valueOf(i));
			}
		};
		client.connect();
	}
	
	/**
	 * @param message
	 *            the message to send through the connection.
	 */
	public void send(String message) {
		client.send(message);
	}
	
	/**
	 * @return whether the client is open.
	 */
	public boolean isOpen() {
		return client.isOpen();
	}
	
	/**
	 * Override this to process a message.
	 * 
	 * @param message
	 *            - the message.
	 */
	public abstract void onMessage(String message);
}

package net.xqhs.flash.wsRegions;

import java.net.URI;
import java.util.Arrays;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import net.xqhs.util.logging.Logger;
import net.xqhs.util.logging.LoggerClassic;

public abstract class WSClient {
	public enum ConnectionState {
		CONNECTING_INITIAL,
		
		CONNECTED,
		
		INITIAL_CONNECTION_FAILED
	}
	
	Logger					log;
	URI						server;
	long					space;
	ConnectionState			state;
	WebSocketClient			client;
	protected final String	logPre	= "<WSClient>";
	
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
		if(!client.isOpen()) {
			// client.close();
			state = ConnectionState.INITIAL_CONNECTION_FAILED;
			log.le("<WSClient> Connection to [] FAILED.", server);
		}
	}
	
	protected void createClient() {
		client = new WebSocketClient(server) {
			
			@Override
			public void onOpen(ServerHandshake arg0) {
				getInstance().onOpen(arg0);
			}
			
			@Override
			public void onMessage(String arg0) {
				getInstance().onMessage(arg0);
			}
			
			@Override
			public void onError(Exception arg0) {
				getInstance().onError(arg0);
			}
			
			@Override
			public void onClose(int arg0, String arg1, boolean arg2) {
				getInstance().onClose(arg0, arg1, arg2);
			}
		};
		client.connect();
	}
	
	protected WSClient getInstance() {
		return this;
	}
	
	public void onOpen(ServerHandshake arg0) {
		log.li("<WSClient> Connection to [] succeeded.", server);
	}
	
	public void onClose(int i, String s, boolean b) {
		log.lw("Closed with exit code " + i);
		log.lw("<WSClient> Connection to [] closed with code [].", server, i);
		
	}
	
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
	
	public void send(String text) {
		client.send(text);
	}
	
	public boolean isOpen() {
		return client.isOpen();
	}
	
	public abstract void onMessage(String message);
}

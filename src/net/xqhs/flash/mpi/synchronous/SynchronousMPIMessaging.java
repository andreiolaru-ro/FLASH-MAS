package net.xqhs.flash.mpi.synchronous;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.ClassicMessagingPylonProxy;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;

public class SynchronousMPIMessaging extends AsynchronousMPIMessaging implements SynchronousMessagingShard {
	
	class ValueAgentWave extends AgentWave {
		/**
		 * The name associated with an auxiliary and optional field (e.g. MPI tag value for filtering messages).
		 * <p>
		 * This is used for MPI support.
		 */
		public final String VALUE = "value";
		
		/**
		 * Creates an agent wave with <b>no</b> destination.
		 * <p>
		 * A <i>complete</i> destination will be added by assembling the elements of the destination..
		 * <p>
		 * This is used for MPI support.
		 *
		 * @param content
		 *            - the content of the wave.
		 * @param value
		 *            - the value of the wave.
		 */
		public ValueAgentWave(String content, int value) {
			super(content);
			add(VALUE, String.valueOf(value));
		}
		
		public ValueAgentWave(String content) {
			super(content);
		}
		
		/**
		 * @return the value of the wave.
		 */
		public int getValue() {
			return Integer.parseInt(getValue(VALUE));
		}
	}
	
	private static final long			serialVersionUID	= 1L;
	private ClassicMessagingPylonProxy	pylon;
	
	public SynchronousMPIMessaging() {
		super();
	}
	
	@Override
	public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
		if(!(context instanceof ClassicMessagingPylonProxy))
			throw new IllegalStateException("Pylon Context is not of expected type.");
		pylon = (ClassicMessagingPylonProxy) context;
		return true;
	}
	
	@Override
	public boolean sendMessage(String source, String destination, String content) {
		if(pylon == null) { // FIXME: use logging
			System.out.println("No pylon added as context.");
			return false;
		}
		
		pylon.send(source, destination, content);
		return true;
	}
	
	@Override
	public ValueAgentWave blockingReceive(String source) {
		try {
			Status status = MPI.COMM_WORLD.probe(Integer.parseInt(source), MPI.ANY_TAG);
			int length = status.getCount(MPI.BYTE);
			byte[] rawMessage = new byte[length];
			MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, Integer.parseInt(source), MPI.ANY_TAG);
			ValueAgentWave wave = new ValueAgentWave(new String(rawMessage), status.getTag());
			wave.addSourceElementFirst(source);
			return wave;
		} catch(MPIException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public ValueAgentWave blockingReceive() {
		try {
			Status status = MPI.COMM_WORLD.probe(MPI.ANY_SOURCE, MPI.ANY_TAG);
			int length = status.getCount(MPI.BYTE);
			int source = status.getSource();
			byte[] rawMessage = new byte[length];
			MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, source, MPI.ANY_TAG);
			ValueAgentWave wave = new ValueAgentWave(new String(rawMessage), status.getTag());
			wave.addSourceElementFirst(String.valueOf(source));
			return wave;
		} catch(MPIException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public ValueAgentWave blockingReceive(String source, int tag) {
		try {
			Status status = MPI.COMM_WORLD.probe(Integer.parseInt(source), tag);
			int length = status.getCount(MPI.BYTE);
			byte[] rawMessage = new byte[length];
			MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, Integer.parseInt(source), tag);
			ValueAgentWave wave = new ValueAgentWave(new String(rawMessage));
			wave.addSourceElementFirst(source);
			return wave;
		} catch(MPIException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	@Override
	public ValueAgentWave blockingReceive(int tag) {
		try {
			Status status = MPI.COMM_WORLD.probe(MPI.ANY_SOURCE, tag);
			int length = status.getCount(MPI.BYTE);
			int source = status.getSource();
			byte[] rawMessage = new byte[length];
			MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, source, tag);
			ValueAgentWave wave = new ValueAgentWave(new String(rawMessage));
			wave.addSourceElementFirst(String.valueOf(source));
			return wave;
		} catch(MPIException e) {
			e.printStackTrace();
		}
		
		return null;
	}
}

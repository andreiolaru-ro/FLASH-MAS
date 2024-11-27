package net.xqhs.flash.mpi.asynchronous;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.ClassicMessagingPylonProxy;

public class AsynchronousMPIMessaging extends AbstractMessagingShard {

    private static final long	serialVersionUID	= 1L;
	private ClassicMessagingPylonProxy	pylon;
    private Thread thread;
    private Status status;
    private byte[] buffer;
    private int source;
    private int tag;
    private static boolean stopFlag = false;

    public int getSource() {
        return source;
    }

    public int getTag() {
        return tag;
    }

    public void setSource(int source) {
        this.source = source;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    private void createMessageReceivingThread() {
        thread = new Thread() {
            private void receiveAsynchronousMessage() {

                AgentWave wave = new AgentWave(new String(buffer));
                wave.addSourceElementFirst(String.valueOf(status.getSource()));
//                AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
//                event.addObject(KEY, wave);
//                getAgent().postAgentEvent(event);
                getAgent().postAgentEvent(wave);
            }

            @Override
            public void run() {
                while (!stopFlag) {
                    try {
                        status = MPI.COMM_WORLD.iProbe(source, tag);
                        if (status != null) {
                            int length = status.getCount(MPI.BYTE);
                            buffer = new byte[length];
                            MPI.COMM_WORLD.recv(buffer, length, MPI.BYTE, source, tag);
                            receiveAsynchronousMessage();
                            status = null;
                        }
                    } catch (MPIException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public AsynchronousMPIMessaging() {
        super();
        source = MPI.ANY_SOURCE;
        tag = MPI.ANY_TAG;
    }

    @Override
    public String extractAgentAddress(String endpoint) {
        return null;
    }

    @Override
    public String getAgentAddress() {
        return null;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
    {
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
    public void signalAgentEvent(AgentEvent event) {
        if(event.getType().equals(AgentEvent.AgentEventType.AGENT_START)) {
            createMessageReceivingThread();
            thread.start();
        } else if(event.getType().equals(AgentEvent.AgentEventType.AGENT_STOP)) {
            try {
                stopFlag = true;
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
	
	@Override
	public void register(String entityName) {
		throw new UnsupportedOperationException();
	}
}

package net.xqhs.flash.mpi.asynchronous;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.AbstractMessagingShard;
import net.xqhs.flash.core.support.MessagingPylonProxy;

import java.nio.ByteBuffer;
import static stefania.TreasureHunt.util.Constants.KEY;

public class AsynchronousMPIMessaging extends AbstractMessagingShard {

    private static final long	serialVersionUID	= 1L;
    private MessagingPylonProxy pylon;
    private Thread thread;
    private Status status;
    private ByteBuffer buffer;
    private static int source;
    private static int tag;
    private static boolean stopFlag = false;

    public static int getSource() {
        return source;
    }

    public static int getTag() {
        return tag;
    }

    public static void setSource(int source) {
        AsynchronousMPIMessaging.source = source;
    }

    public static void setTag(int tag) {
        AsynchronousMPIMessaging.tag = tag;
    }

    private static String byteBuffer_to_String(ByteBuffer buff){
        byte[] bytes;
        if (buff.hasArray()) {
            bytes = buff.array();
        } else {
            bytes = new byte[buff.remaining()];
            buff.get(bytes);
        }
        return new String(bytes);
    }

    private void createMessageReceivingThread() {
        thread = new Thread() {
            private void receiveAsynchronousMessage() {

                String msg = byteBuffer_to_String(buffer);

                AgentWave wave = new AgentWave(msg);
                wave.addSourceElementFirst(String.valueOf(status.getSource()));
                AgentEvent event = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
                event.addObject(KEY, wave);
                getAgent().postAgentEvent(event);
            }

            @Override
            public void run() {
                while (!stopFlag) {
                    try {
                        status = MPI.COMM_WORLD.iProbe(source, tag);
                        if (status != null) {
                            int length = status.getCount(MPI.BYTE);
                            buffer = ByteBuffer.allocateDirect(length);
                            MPI.COMM_WORLD.iRecv(buffer, length, MPI.BYTE, source, tag);
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
        createMessageReceivingThread();
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
        if(!(context instanceof MessagingPylonProxy))
            throw new IllegalStateException("Pylon Context is not of expected type.");
        pylon = (MessagingPylonProxy) context;
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
}

package net.xqhs.flash.mpi.synchronous;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;

public class SynchronousMPIMessaging extends AsynchronousMPIMessaging implements SynchronousMessagingShard {

    private static final long	serialVersionUID	= 1L;
    private MessagingPylonProxy pylon;

    public SynchronousMPIMessaging() {
        super();
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
    public AgentWave blockingReceive(String source) {
        try {
            Status status = MPI.COMM_WORLD.probe(Integer.parseInt(source), MPI.ANY_TAG);
            int length = status.getCount(MPI.BYTE);
            byte[] rawMessage = new byte[length];
            MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, Integer.parseInt(source), MPI.ANY_TAG);
            AgentWave wave = new AgentWave(new String(rawMessage), status.getTag());
            wave.addSourceElementFirst(source);
            return wave;
        } catch (MPIException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public AgentWave blockingReceive() {
        try {
            Status status = MPI.COMM_WORLD.probe(MPI.ANY_SOURCE, MPI.ANY_TAG);
            int length = status.getCount(MPI.BYTE);
            int source = status.getSource();
            byte[] rawMessage = new byte[length];
            MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, source, MPI.ANY_TAG);
            AgentWave wave = new AgentWave(new String(rawMessage), status.getTag());
            wave.addSourceElementFirst(String.valueOf(source));
            return wave;
        } catch (MPIException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public AgentWave blockingReceive(String source, int tag) {
        try {
            Status status = MPI.COMM_WORLD.probe(Integer.parseInt(source), tag);
            int length = status.getCount(MPI.BYTE);
            byte[] rawMessage = new byte[length];
            MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, Integer.parseInt(source), tag);
            AgentWave wave = new AgentWave(new String(rawMessage));
            wave.addSourceElementFirst(source);
            return wave;
        } catch (MPIException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public AgentWave blockingReceive(int tag) {
        try {
            Status status = MPI.COMM_WORLD.probe(MPI.ANY_SOURCE, tag);
            int length = status.getCount(MPI.BYTE);
            int source = status.getSource();
            byte[] rawMessage = new byte[length];
            MPI.COMM_WORLD.recv(rawMessage, length, MPI.BYTE, source, tag);
            AgentWave wave = new AgentWave(new String(rawMessage));
            wave.addSourceElementFirst(String.valueOf(source));
            return wave;
        } catch (MPIException e) {
            e.printStackTrace();
        }

        return null;
    }
}

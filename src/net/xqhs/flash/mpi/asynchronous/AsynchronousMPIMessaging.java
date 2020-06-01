package net.xqhs.flash.mpi.asynchronous;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.util.config.Config;
import net.xqhs.util.config.Configurable;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

import static stefania.TreasureHunt.util.Constants.END_GAME;
import static stefania.TreasureHunt.util.Constants.MPITagValue;

public class AsynchronousMPIMessaging implements MessagingShard {

    private static final long	serialVersionUID	= 1L;
    private MessagingPylonProxy pylon;
    private static ArrayList<AgentWave> messageQueue = new ArrayList<>();
    private Thread thread;
    private Status status;
    private ByteBuffer buffer;
    private static int source;
    private static Semaphore semaphoreEmpty = new Semaphore(1);
    private static Semaphore semaphoreFull = new Semaphore(0);
    private static boolean stopFlag = false;

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
                try {
                    semaphoreEmpty.acquire();

                    String msg = byteBuffer_to_String(buffer);

                    if (msg.equals(END_GAME)) {
                        stopFlag = true;
                    }

                    messageQueue.add(new AgentWave(msg));
                    semaphoreFull.release();

                } catch (InterruptedException e) {
                    System.out.println("Exception here!");
                    e.printStackTrace();
                }
            }

            @Override
            public void run() {
                while (!stopFlag) {
                    try {
                        status = MPI.COMM_WORLD.probe(source, MPITagValue);
                        int length = status.getCount(MPI.BYTE);
                        buffer = ByteBuffer.allocateDirect(length);
                        MPI.COMM_WORLD.iRecv(buffer, length, MPI.BYTE, source, MPITagValue);
                        receiveAsynchronousMessage();
                    } catch (MPIException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
    }

    public AsynchronousMPIMessaging(int source) {
        super();
        this.source = source;
        createMessageReceivingThread();
    }

    public static AgentWave getMessage() {
        AgentWave wave = null;

        try {
            semaphoreFull.acquire();
            wave = messageQueue.remove(0);
            semaphoreEmpty.release();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return wave;
    }

    @Override
    public boolean start() {
        thread.start();
        return true;
    }

    @Override
    public boolean stop() {
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
    public boolean addContext(EntityProxy<Agent> context) {
        return false;
    }

    @Override
    public boolean removeContext(EntityProxy<Agent> context) {
        return false;
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
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return false;
    }

    @Override
    public <C extends Entity<Agent>> EntityProxy<C> asContext() {
        return null;
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
    public String getAgentAddress() {
        return null;
    }

    @Override
    public AgentShardDesignation getShardDesignation() {
        return null;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {

    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        return false;
    }

    @Override
    public Configurable makeDefaults() {
        return null;
    }

    @Override
    public Config lock() {
        return null;
    }

    @Override
    public Config build() {
        return null;
    }

    @Override
    public void ensureLocked() {

    }

    @Override
    public void locked() throws Config.ConfigLockedException {

    }
}

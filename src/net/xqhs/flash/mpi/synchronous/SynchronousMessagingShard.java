package net.xqhs.flash.mpi.synchronous;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.MessagingShard;

public interface SynchronousMessagingShard extends MessagingShard {
    public AgentWave blockingReceive(String source);
    public AgentWave blockingReceive();
    public AgentWave blockingReceive(String source, int tag);
    public AgentWave blockingReceive(int tag);
}

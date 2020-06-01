package net.xqhs.flash.mpi.synchronous;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.MessagingShard;

public interface SynchronousMessagingShard extends MessagingShard {
    public AgentWave blockingReceive(String source, String destination);
}

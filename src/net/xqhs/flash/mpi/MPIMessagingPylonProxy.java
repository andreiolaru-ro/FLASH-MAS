package net.xqhs.flash.mpi;

import net.xqhs.flash.core.support.PylonProxy;

public interface MPIMessagingPylonProxy extends PylonProxy {

    boolean send(String destination, String content, int tag);
    boolean send(String destination, int content, int tag);

    String receive(String source, int messageLength, mpi.Datatype datatype, int tag);
    int receive(String source, int tag);
}

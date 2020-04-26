package net.xqhs.flash.mpi;

import net.xqhs.flash.core.support.PylonProxy;

public interface MPIMessagingPylonProxy extends PylonProxy {

    boolean send(String source, String destination, String content);

    String receive(String source, int messageLength, int tag, mpi.Datatype datatype);
}

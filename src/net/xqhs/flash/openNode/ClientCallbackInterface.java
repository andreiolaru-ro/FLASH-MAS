package net.xqhs.flash.openNode;

import java.rmi.RemoteException;

public interface ClientCallbackInterface {
    void notifyAgentAdded(String agentName) throws RemoteException;
}


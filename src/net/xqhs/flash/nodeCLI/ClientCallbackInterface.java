package net.xqhs.flash.nodeCLI;

import java.rmi.RemoteException;

public interface ClientCallbackInterface {
    void notifyAgentAdded(String agentName) throws RemoteException;
}


package net.xqhs.flash.core.node.clientApp;

import java.rmi.RemoteException;

public interface ClientCallbackInterface {
    void notifyAgentAdded(String agentName) throws RemoteException;
}


package net.xqhs.flash.core.node.clientApp;

import net.xqhs.flash.core.util.MultiTreeMap;

import java.rmi.Remote;


public interface NodeInterface extends Remote {
        MultiTreeMap getNodeConfigurable();
}


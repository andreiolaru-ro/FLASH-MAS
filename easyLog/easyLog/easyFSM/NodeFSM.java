package easyLog.easyFSM;

public class NodeFSM {
    public enum State {
        INITIAL, CONFIGURED, READY, RUNNING, STOPPING, STOPPED
    }

    private String nodeName;
    private State currentState;

    public NodeFSM(String nodeName) {
        this.nodeName = nodeName;
    }

    public void transitionTo(State newState) {
        System.out.println("Node [" + nodeName + "] transitioning from " + currentState + " to " + newState);
        this.currentState = newState;
    }

    public State getCurrentState() {
        return currentState;
    }

    public String getNodeName() {
        return nodeName;
    }

}

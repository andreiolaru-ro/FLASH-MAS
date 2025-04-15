package easyLog.easyFSM;

public class AgentFSM {

    public enum State {
        INITIAL, LOADED, STARTING, RUNNING, STOPPING, STOPPED
    }

    private final String agentName;
    private final String agentId;
    private State currentState;

    public AgentFSM(String agentName, String agentId) {
        this.agentName = agentName;
        this.agentId = agentId;
    }

    public void transitionTo(State newState) {
        // optional de adaugat validari pentru diferite tranzitii + logging?
        System.out.println("Agent [" + agentName + "] transitioning from " + currentState + " to " + newState);
        this.currentState = newState;
    }

    public void setCurrentState(State currentState) {
        this.currentState = currentState;
    }

    public State getCurrentState() {
        return currentState;
    }

    public String getAgentName() {
        return agentName;
    }

    public String getAgentId() {
        return agentId;
    }

}

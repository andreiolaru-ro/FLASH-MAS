package easyLog.easyFSM;

public class PylonFSM {

    public enum State {
        INITIAL, LOADED, RUNNING, STOPPED
    }

    private String pylonId;
    private State currentState;

    public PylonFSM(String pylonId) {
        this.pylonId = pylonId;
    }

    public void transitionTo(State newState) {
        System.out.println("Pylon [" + pylonId + "] transitioning from " + currentState + " to " + newState);
        this.currentState = newState;
    }

    public State getCurrentState() {
        return currentState;
    }

    public String getPylonId() {
        return pylonId;
    }


}

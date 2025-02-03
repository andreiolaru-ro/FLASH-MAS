package automatedTesting;

import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.composite.CompositeAgent;

public class CompositeAgentAccess extends CompositeAgent {

    // Wrapper for AgentState
    public enum PublicAgentState {
        STARTING, RUNNING, STOPPING, STOPPED, TRANSIENT;

        public static PublicAgentState fromAgentState(AgentState state) {
            return PublicAgentState.valueOf(state.name());
        }
    }

    public boolean postAgentEvent(AgentEvent event) {
        return super.postAgentEvent(event);
    }

    public boolean FSMEventOut(AgentEvent.AgentEventType eventType, boolean toFromTransient) {
        return super.FSMEventOut(eventType, toFromTransient);
    }

    public PublicAgentState getCurrentState() {
        return PublicAgentState.fromAgentState(this.agentState);
    }

    public PublicAgentState FSMEventInPublic(AgentEvent.AgentEventType eventType, boolean fromToTransient, boolean createThread) {
        return PublicAgentState.fromAgentState(super.FSMEventIn(eventType, fromToTransient, createThread));
    }

    public AgentEvent eventProcessingCycle() {
        return super.eventProcessingCycle();
    }

    public void clearEventQueue() {
        if (eventQueue != null) {
            eventQueue.clear();
        }
    }
}
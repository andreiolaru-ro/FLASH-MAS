package automatedTesting;

import net.xqhs.flash.core.agent.AgentEvent;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class CompositeAgentTesting {

    private CompositeAgentAccess compositeAgentAccess;

    @Before
    public void setUp() {
        compositeAgentAccess = new CompositeAgentAccess();
    }

    @Test
    public void testFSMEventIn_AGENT_START() {
        // Test AGENT_START event
        CompositeAgentAccess.PublicAgentState result = compositeAgentAccess.FSMEventInPublic(AgentEvent.AgentEventType.AGENT_START, false, true);

        // Verify that the agent state is transitioning to STARTING
        assertEquals(CompositeAgentAccess.PublicAgentState.STARTING, result);
    }

    @Test
    public void testFSMEventIn_AGENT_STOP() {
        // Test AGENT_STOP event
        CompositeAgentAccess.PublicAgentState result = compositeAgentAccess.FSMEventInPublic(AgentEvent.AgentEventType.AGENT_STOP, false, true);

        // Verify that the agent state is transitioning to STOPPING
        assertEquals(CompositeAgentAccess.PublicAgentState.STOPPING, result);
    }

    @Test
    public void testFSMEventOut_AGENT_START() {
        // Set the initial state to STARTING
        compositeAgentAccess.FSMEventInPublic(AgentEvent.AgentEventType.AGENT_START, false, true);

        // Test AGENT_START event
        boolean threadExit = compositeAgentAccess.FSMEventOut(AgentEvent.AgentEventType.AGENT_START, false);

        // Verify that the agent state is now RUNNING
        assertEquals(CompositeAgentAccess.PublicAgentState.RUNNING, compositeAgentAccess.getCurrentState());

        // Verify that the thread should not exit
        assertFalse(threadExit);
    }

    @Test
    public void testFSMEventOut_AGENT_STOP() {
        // Set the initial state to RUNNING
        compositeAgentAccess.FSMEventInPublic(AgentEvent.AgentEventType.AGENT_START, false, true);
        compositeAgentAccess.FSMEventOut(AgentEvent.AgentEventType.AGENT_START, false);

        // Test AGENT_STOP event
        boolean threadExit = compositeAgentAccess.FSMEventOut(AgentEvent.AgentEventType.AGENT_STOP, false);

        // Verify that the agent state is now STOPPED
        assertEquals(CompositeAgentAccess.PublicAgentState.STOPPED, compositeAgentAccess.getCurrentState());

        // Verify that the thread should exit
        assertTrue(threadExit);
    }

    @Test
    public void testFSMEventOut_AGENT_STOP_Transient() {
        // Set the initial state to RUNNING
        compositeAgentAccess.FSMEventInPublic(AgentEvent.AgentEventType.AGENT_START, false, true);
        compositeAgentAccess.FSMEventOut(AgentEvent.AgentEventType.AGENT_START, false);

        // Test AGENT_STOP event with transient parameter
        boolean threadExit = compositeAgentAccess.FSMEventOut(AgentEvent.AgentEventType.AGENT_STOP, true);

        // Verify that the agent state is now TRANSIENT
        assertEquals(CompositeAgentAccess.PublicAgentState.TRANSIENT, compositeAgentAccess.getCurrentState());

        // Verify that the thread should exit
        assertTrue(threadExit);
    }

    @Test
    public void testEventProcessingCycle_EmptyQueue() {
        // Clear the event queue before the test
        compositeAgentAccess.clearEventQueue();

        // Post the event to the queue before starting the cycle
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));

        // Use a separate thread to run the event processing cycle
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            // Wait for the event processing cycle to process the event
            eventProcessingThread.join(2000);  // Wait for 2 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify the agent state transitioned correctly after processing the event
        assertEquals(CompositeAgentAccess.PublicAgentState.RUNNING, compositeAgentAccess.getCurrentState());
    }

    @Test
    public void testEventProcessingCycle_MultipleEvents() {
        // Clear the event queue before the test
        compositeAgentAccess.clearEventQueue();

        // Post the first event (AGENT_START)
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));

        // Post the second event (AGENT_STOP)
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));

        // Use a separate thread to run the event processing cycle
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            // Wait for the event processing cycle to process both events
            eventProcessingThread.join(2000);  // Wait for 2 seconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify the agent's state transitioned correctly after processing the events
        assertEquals(CompositeAgentAccess.PublicAgentState.STOPPED, compositeAgentAccess.getCurrentState());
    }

    @Test
    public void testEventProcessingCycle_MixedEventTypes() {
        // Clear the event queue before the test
        compositeAgentAccess.clearEventQueue();

        // Post a constructive event (AGENT_START has CONSTRUCTIVE sequence type)
        AgentEvent constructiveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_START);
        compositeAgentAccess.postAgentEvent(constructiveEvent);

        // Post an unordered event (AGENT_WAVE has UNORDERED sequence type)
        AgentEvent unorderedEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        compositeAgentAccess.postAgentEvent(unorderedEvent);

        // Post a destructive event (AGENT_STOP has DESTRUCTIVE sequence type)
        AgentEvent destructiveEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        compositeAgentAccess.postAgentEvent(destructiveEvent);

        // Use a separate thread to run the event processing cycle
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            // Wait for the event processing cycle to process events
            eventProcessingThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify the final state is STOPPED after processing all events
        assertEquals(CompositeAgentAccess.PublicAgentState.STOPPED, compositeAgentAccess.getCurrentState());
    }

    @Test
    public void testEventProcessingCycle_DelayedEventArrival() {
        // Clear the event queue before the test
        compositeAgentAccess.clearEventQueue();

        // Create a thread to simulate delayed event arrival
        Thread delayedEventThread = new Thread(() -> {
            try {
                Thread.sleep(1000);  // Wait 1 second before adding the event
                compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        delayedEventThread.start();

        // Use a separate thread to run the event processing cycle
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            delayedEventThread.join(); // Ensure event is posted
            eventProcessingThread.join(3000); // Allow 3 seconds for processing
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Wait until the state transitions from STARTING → RUNNING
        boolean transitionedToRunning = false;
        for (int i = 0; i < 10; i++) { // Retry for up to 1 second
            if (compositeAgentAccess.getCurrentState() == CompositeAgentAccess.PublicAgentState.RUNNING) {
                transitionedToRunning = true;
                break;
            }
            try {
                Thread.sleep(100); // Give 0.1 second for the state to update
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Assert final state
        assertTrue("Agent did not transition to RUNNING", transitionedToRunning);
    }

    @Test
    public void testEventProcessingCycle_ThreadInterrupted() {
        // Clear the event queue before the test
        compositeAgentAccess.clearEventQueue();

        // Post an event to the queue before starting the cycle
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));

        // Start the event processing in a separate thread
        Thread eventProcessingThread = new Thread(() -> {
            try {
                compositeAgentAccess.eventProcessingCycle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        eventProcessingThread.start();

        try {
            // Interrupt the thread while it's processing
            Thread.sleep(500);
            eventProcessingThread.interrupt();
            eventProcessingThread.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Ensure the agent did not crash and is in a stable state (RUNNING or STOPPED)
        assertNotNull(compositeAgentAccess.getCurrentState());
    }

    @Test
    public void testEventProcessingCycle_ReverseDestructiveOrder() {
        // Clear the event queue before the test
        compositeAgentAccess.clearEventQueue();

        // Post a destructive event first
        AgentEvent destructiveEvent1 = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        compositeAgentAccess.postAgentEvent(destructiveEvent1);

        // Post another destructive event
        AgentEvent destructiveEvent2 = new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP);
        compositeAgentAccess.postAgentEvent(destructiveEvent2);

        // Use a separate thread to run the event processing cycle
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            eventProcessingThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify the final state is STOPPED after processing all destructive events
        assertEquals(CompositeAgentAccess.PublicAgentState.STOPPED, compositeAgentAccess.getCurrentState());
    }


    @Test
    public void testEventProcessingCycle_ConcurrentEvents() {
        // Clear the event queue before the test
        compositeAgentAccess.clearEventQueue();

        // Thread 1: Ensure the agent starts first
        Thread eventThread1 = new Thread(() -> {
            compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        });

        // Thread 2: Post an intermediate event (AGENT_WAVE) after some delay
        Thread eventThread2 = new Thread(() -> {
            try {
                Thread.sleep(500); // Wait for START to process
                compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Thread 3: Ensure AGENT_STOP is posted only after AGENT_START is complete
        Thread eventThread3 = new Thread(() -> {
            try {
                Thread.sleep(1000); // Ensure the agent has transitioned to RUNNING
                compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        eventThread1.start();
        eventThread2.start();
        eventThread3.start();

        try {
            eventThread1.join();
            eventThread2.join();
            eventThread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Use a separate thread to run the event processing cycle
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            eventProcessingThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Ensure that AGENT_STOP was processed and the agent is now STOPPED
        assertEquals(CompositeAgentAccess.PublicAgentState.STOPPED, compositeAgentAccess.getCurrentState());
    }

    @Test
    public void testLongRunningEvent() {
        compositeAgentAccess.clearEventQueue();

        // Post AGENT_START
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));

        // Start event processing
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        // Wait until the agent reaches the RUNNING state, with a timeout
        boolean reachedRunning = false;
        for (int i = 0; i < 10; i++) { // 10 attempts, 100ms apart
            if (compositeAgentAccess.getCurrentState() == CompositeAgentAccess.PublicAgentState.RUNNING) {
                reachedRunning = true;
                break;
            }
            try {
                Thread.sleep(100); // Wait before checking again
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        try {
            eventProcessingThread.join(2000); // Ensure the processing thread completes
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Assert that the state is RUNNING after waiting
        assertTrue("Agent did not reach RUNNING state!", reachedRunning);
    }

    @Test
    public void testEventProcessingCycle_RapidSequenceOfEvents() {
        // Clear event queue before testing
        compositeAgentAccess.clearEventQueue();

        // Thread 1: Post AGENT_START
        Thread eventThread1 = new Thread(() -> {
            compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        });

        // Thread 2: Post AGENT_STOP after a short delay to simulate stopping
        Thread eventThread2 = new Thread(() -> {
            try {
                Thread.sleep(500); // Ensure AGENT_START executes first
                compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Thread 3: Post AGENT_START again to simulate restart
        Thread eventThread3 = new Thread(() -> {
            try {
                Thread.sleep(1000); // Ensure AGENT_STOP executes before restarting
                compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        eventThread1.start();
        eventThread2.start();
        eventThread3.start();

        try {
            eventThread1.join();
            eventThread2.join();
            eventThread3.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Start event processing
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            eventProcessingThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Ensure the agent is running after the restart
        assertEquals(CompositeAgentAccess.PublicAgentState.RUNNING, compositeAgentAccess.getCurrentState());
    }

    @Test
    public void testEventProcessingCycle_MixedOrderedUnorderedEvents() {
        // Clear the event queue
        compositeAgentAccess.clearEventQueue();

        // Constructive event
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));

        // Unordered event
        AgentEvent unorderedEvent = new AgentEvent(AgentEvent.AgentEventType.AGENT_WAVE);
        compositeAgentAccess.postAgentEvent(unorderedEvent);

        // Destructive event
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));

        // Run event processing cycle
        Thread eventProcessingThread = new Thread(() -> compositeAgentAccess.eventProcessingCycle());
        eventProcessingThread.start();

        try {
            eventProcessingThread.join(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify the final state (STOPPED due to AGENT_STOP)
        assertEquals(CompositeAgentAccess.PublicAgentState.STOPPED, compositeAgentAccess.getCurrentState());
    }

    @Test
    public void testEventProcessingCycle_InterruptedDuringProcessing() {
        // Clear event queue
        compositeAgentAccess.clearEventQueue();

        // Post a sequence of events
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
        compositeAgentAccess.postAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));

        // Start event processing in a separate thread
        Thread eventProcessingThread = new Thread(() -> {
            try {
                compositeAgentAccess.eventProcessingCycle();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        eventProcessingThread.start();

        try {
            Thread.sleep(500); // Allow some processing
            eventProcessingThread.interrupt(); // Interrupt processing
            eventProcessingThread.join(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Ensure agent did not crash and ended up in a stable state
        CompositeAgentAccess.PublicAgentState finalState = compositeAgentAccess.getCurrentState();
        assertNotNull(finalState);
        assertTrue(finalState == CompositeAgentAccess.PublicAgentState.RUNNING || finalState == CompositeAgentAccess.PublicAgentState.STOPPED);
    }
}

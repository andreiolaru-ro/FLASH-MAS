!start.

+!start <-
    .print("Alice started");
    .send(bob, tell, ping).

// Receive pong, send pang, and shut down
+ pong <-
    .print("Alice received pong");
    .send(bob, tell, pang);
    .print("Alice shutting down");
    .kill_agent.
!start.

+!start <-
    .print("Bob started").

// Receive ping and reply with pong
+ping <-
    .print("Bob received ping");
    .send(alice, tell, pong).

// Receive pang and shut down
+pang <-
    .print("Bob received pang");
    .print("Bob shutting down");
    .kill_agent.
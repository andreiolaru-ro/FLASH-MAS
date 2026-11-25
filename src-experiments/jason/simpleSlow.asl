!start.

+!start <-
    .print("Agent started!");
    !first;
    !second.

+!first <-
    .print("First goal executed immediately.").

+!second <-
    .wait(2500);
    .print("Second goal executed after some seconds.");
    .my_name(Name);
    .kill_agent(Name).
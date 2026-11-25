!start.

+!start <-
    .print("Agent started!");
    !first;
    !second.

+!first <-
    .print("Time zero.").

+!second <-
    .wait(1000);
    .print("Time one.");
    .wait(1000);
    .print("Time two.");
    .wait(1000);
    .print("Time three. Done.");
    .my_name(Name);
    .kill_agent(Name).
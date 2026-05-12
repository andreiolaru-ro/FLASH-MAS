package abms.smartMeeting;

import net.xqhs.flash.FlashBoot;

public class SmartMeetingBoot {
    public static void main(String[] args) {
        String a = "";
        a += " -load_order simulation;executor;context;SmartMeetingGroup";
        a += " -package net.xqhs.flash.abms";
        a += " -package abms.smartMeeting";
        a += " -loader SmartMeetingGroup classpath:abms.smartMeeting.SmartMeetingGroupLoader";
        a += " -node dummy -simulation sim classpath:Simulation";
        a += " -executor StepWise:StepWise steps:60";
        a += " -context AgentManagement:agentManagement";
        a += " -context Random:random seed:42";
        a += " -context GraphCommunication:communication";
        a += " -context Space:space topology:graph"
                + " nodes:lobby,r1,r2,r3,r4,r5,r6"
                + " edges:lobby-r1,lobby-r2,lobby-r3,r1-r4,r3-r5,r5-r6";
        a += " -SmartMeetingGroup g";
        a += " -agent Auction n:1 releaseAfterSteps:20";
        a += " -agent Room n:6 capacity:8 equipment:PROJECTOR,WHITEBOARD,VIDEO_CONFERENCE";
        a += " -agent Person n:3 auctionAgent:auction0";

        FlashBoot.main(a.split(" "));
    }
}

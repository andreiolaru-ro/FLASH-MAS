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
        a += " -executor StepWise:StepWise steps:40";
        a += " -context AgentManagement:agentManagement";
        a += " -context Random:random seed:24";
        a += " -context ProximityCommunication:communication";
        a += " -context Space:space width:7 height:7";
        a += " -SmartMeetingGroup g";
        a += " -agent Auction n:1 visionRange:6 bidWaitSteps:2 cooldownSteps:3 releaseAfterSteps:8";
        a += " -agent Room n:6 capacity:8 visionRange:6 equipment:PROJECTOR,WHITEBOARD,VIDEO_CONFERENCE";

        FlashBoot.main(a.split(" "));
    }

    // agent has vision range because at the moment smart meeting scenario is grid based;
    // therefore agent does not see all rooms
}

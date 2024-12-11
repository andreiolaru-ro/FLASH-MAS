package net.xqhs.flash.testViorel;


import net.xqhs.flash.core.node.clientApp.ClientApp;

import java.util.concurrent.TimeUnit;

public class Timer {
    private final Timer timer = new Timer();

    /*public void scheduledLoadingTask(String[] argset){
        timer.scheduleTask(() -> {
            System.out.println("Loading Timer");
            PartialCLIWrapp.processArgs(argset);
        }, 2, TimeUnit.SECONDS);

        //Stop timer
       // Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }*/
    public void scheduledLoadingTask(String[] argset){
        timer.scheduleTask(() -> {
            System.out.println("Loading Timer");
            try {
                ClientApp clientApp = ClientApp.getInstance();
                clientApp.notifyAgentAdded("AgentExampleTest");

                // CliProcess
                // PartialCLIWrapp.processArgs(argset);
            } catch (Exception e){
                System.out.println("Error in scheduled task: " + e.getMessage());
            }

        }, 2, TimeUnit.SECONDS);

        //Stop timer
        // Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void scheduleTask(Runnable task, long delay, TimeUnit unit){
        timer.scheduleTask(task,delay,unit);
    }
    //public void shutdown(){}
    //timer.shutdown();
}

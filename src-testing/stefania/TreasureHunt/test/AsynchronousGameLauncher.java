package stefania.TreasureHunt.test;

import mpi.*;
import net.xqhs.flash.mpi.MPISupport;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;
import stefania.TreasureHunt.agents.asynchonous.AsynchronousMasterAgent;
import stefania.TreasureHunt.agents.asynchonous.AsynchronousPlayerAgent;

public class AsynchronousGameLauncher {

    public static void main(String[] args) throws MPIException {
        MPI.Init(args) ;

        int rank = MPI.COMM_WORLD.getRank() ;
        int size = MPI.COMM_WORLD.getSize() ;

        MPISupport pylon = new MPISupport();

        if (rank == 0) {
            AsynchronousMasterAgent asynchronousMasterAgent = new AsynchronousMasterAgent("Agent" + rank, rank, size);
            asynchronousMasterAgent.addContext(pylon.asContext());
            AsynchronousMPIMessaging asynchronousMPIMessaging = new AsynchronousMPIMessaging();
            asynchronousMPIMessaging.addContext(asynchronousMasterAgent.asContext());
            asynchronousMasterAgent.addMessagingShard(asynchronousMPIMessaging);
            asynchronousMasterAgent.start();
        } else {
            AsynchronousPlayerAgent asynchronousPlayerAgent = new AsynchronousPlayerAgent("Agent" + rank, rank, size);
            asynchronousPlayerAgent.addContext(pylon.asContext());
            AsynchronousMPIMessaging asynchronousMPIMessaging = new AsynchronousMPIMessaging();
            asynchronousMPIMessaging.addContext(asynchronousPlayerAgent.asContext());
            asynchronousPlayerAgent.addMessagingShard(asynchronousMPIMessaging);
            asynchronousPlayerAgent.start();
        }

        MPI.Finalize();
    }
}

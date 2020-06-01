package stefania.TreasureHunt.test;

import mpi.*;
import net.xqhs.flash.mpi.MPISupport;
import net.xqhs.flash.mpi.synchronous.SynchronousMPIMessaging;
import stefania.TreasureHunt.agents.synchronous.SynchronousMasterAgent;
import stefania.TreasureHunt.agents.synchronous.SynchronousPlayerAgent;

public class SynchronousGameLauncher {

    public static void main(String[] args) throws MPIException {
        MPI.Init(args) ;

        int rank = MPI.COMM_WORLD.getRank() ;
        int size = MPI.COMM_WORLD.getSize() ;

        MPISupport pylon = new MPISupport();

        if (rank == 0) {
            SynchronousMasterAgent synchronousMasterAgent = new SynchronousMasterAgent("Agent" + rank, rank, size);
            synchronousMasterAgent.addContext(pylon.asContext());
            synchronousMasterAgent.addMessagingShard(new SynchronousMPIMessaging());
            synchronousMasterAgent.start();
        } else {
            SynchronousPlayerAgent playerAgent = new SynchronousPlayerAgent("Agent" + rank, rank, size);
            playerAgent.addContext(pylon.asContext());
            playerAgent.addMessagingShard(new SynchronousMPIMessaging());
            playerAgent.start();
        }

        MPI.Finalize();
    }
}

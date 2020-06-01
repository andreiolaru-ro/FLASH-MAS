package stefania.TreasureHunt.test;

import mpi.*;
import net.xqhs.flash.mpi.MPISupport;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;
import stefania.TreasureHunt.agents.asynchonous.AsynchronousMasterAgent;
import stefania.TreasureHunt.agents.asynchonous.AsynchronousPlayerAgent;
import static stefania.TreasureHunt.util.Constants.*;

public class AsynchronousGameLauncher {

    public static void main(String[] args) throws MPIException {
        MPI.Init(args) ;

        int rank = MPI.COMM_WORLD.getRank() ;
        int size = MPI.COMM_WORLD.getSize() ;

        MPISupport pylon = new MPISupport();

        if (rank == 0) {
            AsynchronousMasterAgent asynchronousMasterAgent = new AsynchronousMasterAgent("Agent" + rank, rank, size);
            asynchronousMasterAgent.addContext(pylon.asContext());
            asynchronousMasterAgent.addMessagingShard(new AsynchronousMPIMessaging(Integer.parseInt(PLAYER)));
            asynchronousMasterAgent.start();
        } else {
            AsynchronousPlayerAgent asynchronousPlayerAgent = new AsynchronousPlayerAgent("Agent" + rank, rank, size);
            asynchronousPlayerAgent.addContext(pylon.asContext());
            asynchronousPlayerAgent.addMessagingShard(new AsynchronousMPIMessaging(Integer.parseInt(MASTER)));
            asynchronousPlayerAgent.start();
        }

        MPI.Finalize();
    }
}

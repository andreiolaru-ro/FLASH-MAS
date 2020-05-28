package stefania.TreasureHuntTest;

import mpi.*;
import net.xqhs.flash.mpi.MPISupport;
import stefania.TreasureHunt.agents.MasterAgent;
import stefania.TreasureHunt.agents.PlayerAgent;

public class GameLauncher {

    public static void main(String[] args) throws MPIException {
        MPI.Init(args) ;

        int rank = MPI.COMM_WORLD.getRank() ;
        int size = MPI.COMM_WORLD.getSize() ;

        MPISupport pylon = new MPISupport();

        if (rank == 0) {
            MasterAgent masterAgent = new MasterAgent("Agent" + rank, rank, size);
            masterAgent.addContext(pylon.asContext());
            masterAgent.addMessagingShard(new MPISupport.MPIMessaging());
            masterAgent.start();
        } else {
            PlayerAgent playerAgent = new PlayerAgent("Agent" + rank, rank, size);
            playerAgent.addContext(pylon.asContext());
            playerAgent.addMessagingShard(new MPISupport.MPIMessaging());
            playerAgent.start();
        }

        MPI.Finalize();
    }
}

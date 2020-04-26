package stefania;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.Pylon;

import mpi.* ;
import net.xqhs.flash.mpi.MPIMessagingPylonProxy;
import net.xqhs.flash.mpi.MPISupport;

@SuppressWarnings("javadoc")
class TestMPIAgent implements Agent {
    private String					name;
    private MPISupport.MPIMessaging messagingShard;
    private MPIMessagingPylonProxy pylon;
    private int myRank;
    private int size;

    public ShardContainer proxy	= new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) { }

        @Override
        public String getEntityName()
        {
            return getName();
        }

        @Override
        public AgentShard getAgentShard(AgentShardDesignation designation)
        {
            // not supported
            return null;
        }

    };

    public TestMPIAgent(String name, int rank, int size) {
        this.name = name;
        this.myRank = rank;
        this.size = size;

        System.out.println("I am " + name + " with rank " + this.myRank);
    }

    @Override
    public boolean start() {

        if (myRank != 0) {
            this.messagingShard.sendMessage("" + this.myRank, "0", "Hello from " + this.myRank);
            String response = this.messagingShard.receiveMessage("0", 3, 0, MPI.CHAR);
            System.out.println("[" + this.myRank + "] " + response);
        } else {
            String message;
            String ack = "ACK";

            for (int i = 1; i < this.size; i++) {
                message = this.messagingShard.receiveMessage("" + i, 12, 0,  MPI.CHAR);
                System.out.println("Master received: " + message);
                this.messagingShard.sendMessage("" + this.myRank, "" + i, ack);
            }
        }

        return true;
    }

    @Override
    public boolean stop()
    {
        return true;
    }

    @Override
    public boolean isRunning()
    {
        return true;
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context)
    {
        pylon = (MPIMessagingPylonProxy) context;
        if(messagingShard != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context)
    {
        return true;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context)
    {
        return true;
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context)
    {
        pylon = null;
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Agent> asContext() {
        return proxy;
    }

    public boolean addMessagingShard(MPISupport.MPIMessaging shard)
    {
        messagingShard = shard;
        shard.addContext(proxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }
}

@SuppressWarnings("javadoc")
public class Main
{
    public static void main(String[] args) throws MPIException {
        MPI.Init(args) ;

        int rank = MPI.COMM_WORLD.getRank() ;
        int size = MPI.COMM_WORLD.getSize() ;

        MPISupport pylon = new MPISupport();

        TestMPIAgent agent = new TestMPIAgent("Agent" + rank, rank, size);
        agent.addContext(pylon.asContext());
        agent.addMessagingShard(new MPISupport.MPIMessaging());
        agent.start();

        MPI.Finalize();
    }
}

package stefania.TreasureHunt.agents.asynchonous;

import mpi.MPI;
import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.mpi.asynchronous.AsynchronousMPIMessaging;
import stefania.TreasureHunt.util.Coord;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import static stefania.TreasureHunt.util.Constants.*;

public class AsynchronousMasterAgent implements Agent {
    private String					name;
    public AsynchronousMPIMessaging messagingShard;
    private static LinkedBlockingQueue<AgentWave> messageQueue;
    private MessagingPylonProxy pylon;
    private Coord treasure;
    private Coord playerPos;
    public int myRank;
    public int size;

    public ShardContainer proxy	= new ShardContainer() {
        @Override
        public void postAgentEvent(AgentEvent event) {
            AgentWave wave = (AgentWave) event.getObject(KEY);
            synchronized (messageQueue) {
                try {
                        messageQueue.put(wave);
                        messageQueue.notify();
                } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
        }

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

    public static AgentWave getMessage() {
        AgentWave wave = null;

        synchronized(messageQueue)
        {
            if(messageQueue.isEmpty())
                try
                {
                    messageQueue.wait();
                } catch(InterruptedException e)
                {
                    // do nothing
                }
            if(!messageQueue.isEmpty())
                wave = messageQueue.poll();
        }

        return wave;
    }

    public AsynchronousMasterAgent(String name, int rank, int size) {
        messageQueue = new LinkedBlockingQueue<>();
        this.name = name;
        this.myRank = rank;
        this.size = size;
    }

    public void initGame() {
        Random generator = new Random();
        setTreasure(new Coord(generator.nextInt(9)+1, generator.nextInt(9)+1));
        playerPos = new Coord(5, 5);
    }

    public String evaluateProximity(String playerMoveDirection) {
        Coord newPlayerPos = playerPos.clone();
        newPlayerPos.move(playerMoveDirection);
        String hint = "colder";
        if(newPlayerPos.distanceTo(treasure)==0)
            hint = "win";
        else if (newPlayerPos.distanceTo(getTreasure())<playerPos.distanceTo(getTreasure()))
            hint = "warmer";
        else if(newPlayerPos.distanceTo(getTreasure())>playerPos.distanceTo(getTreasure()))
            hint = "colder";
        else
            hint = "same";

        playerPos = newPlayerPos;
        return hint;
    }

    public Coord getTreasure() {
        return treasure;
    }

    public void setTreasure(Coord treasure) {
        this.treasure = treasure;
    }

    @Override
    public boolean start() {
        AsynchronousMasterInitBehaviour initBehaviour = new AsynchronousMasterInitBehaviour(this);
        AsynchronousMasterPlayBehaviour playBehaviour = new AsynchronousMasterPlayBehaviour(this);
        AsynchronousMasterEndBehaviour endBehaviour = new AsynchronousMasterEndBehaviour(this);

        initBehaviour.action();

        while (playBehaviour.nextState != 0) {
            playBehaviour.action();
        }

        endBehaviour.action();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public boolean isRunning() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean addContext(EntityProxy<Pylon> context)
    {
        pylon = (MessagingPylonProxy) context;
        if(messagingShard != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    @Override
    public boolean removeContext(EntityProxy<Pylon> context)
    {
        pylon = null;
        return true;
    }

    @Override
    public boolean addGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return true;
    }

    @Override
    public boolean removeGeneralContext(EntityProxy<? extends Entity<?>> context) {
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public EntityProxy<Agent> asContext() {
        return proxy;
    }

    public boolean addMessagingShard(AsynchronousMPIMessaging shard)
    {
        messagingShard = shard;
        shard.addContext(proxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    public static class AsynchronousMasterInitBehaviour {
        static AsynchronousMasterAgent masterAgent;

        public AsynchronousMasterInitBehaviour(AsynchronousMasterAgent masterAgent) {
            this.masterAgent = masterAgent;
        }

        public static void action() {
            String message = "START GAME";

            masterAgent.initGame();
            masterAgent.messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
            masterAgent.messagingShard.setSource(Integer.parseInt(PLAYER));
//            masterAgent.messagingShard.setTag(5);
            masterAgent.messagingShard.sendMessage(MASTER, PLAYER,  message);
        }
    }

    public static class AsynchronousMasterPlayBehaviour {
        AsynchronousMasterAgent masterAgent;
        int nextState;

        public AsynchronousMasterPlayBehaviour(AsynchronousMasterAgent masterAgent) {
            this.masterAgent = masterAgent;
            this.nextState = 1;
        }

        public void action() {
            String playerMoveDirection;
            String hint;

            AgentWave wave = getMessage();
            playerMoveDirection = wave.getContent();
            System.out.println("Master received: " + playerMoveDirection);

            hint = masterAgent.evaluateProximity(playerMoveDirection);

            if (hint.equals("win")) {
                 nextState = 0;
            }

            masterAgent.messagingShard.sendMessage(MASTER, wave.getCompleteSource(), hint);
        }

        public int onEnd() {
            return nextState;
        }
    }



    public static class AsynchronousMasterEndBehaviour {
        AsynchronousMasterAgent masterAgent;

        public AsynchronousMasterEndBehaviour(AsynchronousMasterAgent masterAgent) {
            this.masterAgent = masterAgent;
        }

        public void action() {
            System.out.println(masterAgent.getName() + "> The player found the treasure at " + masterAgent.getTreasure() + "!");
            masterAgent.messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
        }
    }
}

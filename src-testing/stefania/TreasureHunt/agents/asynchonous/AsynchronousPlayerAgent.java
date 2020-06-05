package stefania.TreasureHunt.agents.asynchonous;

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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

import static stefania.TreasureHunt.util.Constants.*;

public class AsynchronousPlayerAgent implements Agent {
    private String					name;
    private AsynchronousMPIMessaging messagingShard;
    private MessagingPylonProxy pylon;
    private static LinkedBlockingQueue<AgentWave> messageQueue;
    private int myRank;
    private int size;
    private Coord oldPos;
    private Coord pos;
    private List<Coord> candidates;

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

    public AsynchronousPlayerAgent(String name, int rank, int size) {
        messageQueue = new LinkedBlockingQueue<>();
        this.name = name;
        this.myRank = rank;
        this.size = size;
    }

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

    public void initGame() {
        setPos(new Coord(5, 5));
//        setPos(new Coord(myRank, myRank));
        candidates = new ArrayList<Coord>();
        for (int i = 1; i <= 11; i++) {
            for (int j = 1; j <= 11; j++) {
                candidates.add(new Coord(i, j));
            }
        }
    }

    public String determineNextMove(String hint) {
        if(hint.equals("win")) return "win";
        eliminateCandidates(hint);
        Coord target = candidates.get(0);
        // if player is on target, they can only be sure they're
        // on the treasure if it's the only target left
        if(pos.equals(target)){
            //if player has found treasure, the win
            if(candidates.size()==1)
                return "win";
                // if not, the player moves on to get more indications
            else{
                target = candidates.get(1);
            }
        }

        return getPos().determineDirectionTo(target);
    }

    private void eliminateCandidates(String hint){
        List<Coord> toRemove = new ArrayList<Coord>();
        switch(hint){
            //if player got warmer, they can remove all
            //candidates they got farther away from
            case "warmer":
                for (Coord c : candidates) {
                    if(getPos().distanceTo(c) > oldPos.distanceTo(c))
                        toRemove.add(c);
                }
                break;
            //if player got colder, they can remove all
            //candidates they got closer to
            case "colder":
                for (Coord c : candidates) {
                    if(getPos().distanceTo(c) < oldPos.distanceTo(c))
                        toRemove.add(c);
                }
                break;
            default: // case "same"
                break;
        }
        candidates.removeAll(toRemove);
    }

    public void move(String direction) {
        oldPos = getPos().clone();
        getPos().move(direction);
    }

    public Coord getPos() {
        return pos;
    }

    public void setPos(Coord pos) {
        this.pos = pos;
    }

    @Override
    public boolean start() {
        AsynchronousPlayerInitBehaviour initBehaviour = new AsynchronousPlayerInitBehaviour(this);
        AsynchronousPlayerPlayBehaviour playBehaviour = new AsynchronousPlayerPlayBehaviour(this);
        AsynchronousPlayerEndBehaviour endBehaviour = new AsynchronousPlayerEndBehaviour(this);

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

    public static class AsynchronousPlayerInitBehaviour {
        static AsynchronousPlayerAgent playerAgent;

        public AsynchronousPlayerInitBehaviour(AsynchronousPlayerAgent playerAgent) {
            this.playerAgent = playerAgent;
        }

        public static void action() {
            String hintRequest = "up";

            playerAgent.messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_START));
            playerAgent.messagingShard.setSource(Integer.parseInt(MASTER));
//            playerAgent.messagingShard.setTag(5);
            getMessage();

            playerAgent.initGame();
            playerAgent.move("up");

            playerAgent.messagingShard.sendMessage(PLAYER, MASTER, hintRequest);
        }
    }

    public static class AsynchronousPlayerPlayBehaviour {
        AsynchronousPlayerAgent playerAgent;
        int nextState;

        public AsynchronousPlayerPlayBehaviour(AsynchronousPlayerAgent playerAgent) {
            this.playerAgent = playerAgent;
            this.nextState = 1;
        }

        public void action() {
            String moveDirection;
            String hint;

            hint = getMessage().getContent();

            System.out.println(playerAgent.getName() + " pos: " + playerAgent.getPos());

            moveDirection = playerAgent.determineNextMove(hint);

            if(moveDirection.equals("win"))
                nextState = 0;

            playerAgent.move(moveDirection);

            playerAgent.messagingShard.sendMessage(PLAYER, MASTER, moveDirection);
        }

        public int onEnd() {
            return nextState;
        }
    }

    public static class AsynchronousPlayerEndBehaviour {
        AsynchronousPlayerAgent playerAgent;

        public AsynchronousPlayerEndBehaviour(AsynchronousPlayerAgent playerAgent) {
            this.playerAgent = playerAgent;
        }

        public void action() {
            System.out.println(playerAgent.getName() + "> I found the treasure at " + playerAgent.getPos() + "!");
            playerAgent.messagingShard.signalAgentEvent(new AgentEvent(AgentEvent.AgentEventType.AGENT_STOP));
        }
    }
}

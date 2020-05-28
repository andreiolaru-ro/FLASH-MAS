package stefania.TreasureHunt.agents;

import net.xqhs.flash.core.Entity;
import net.xqhs.flash.core.agent.Agent;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShard;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.ShardContainer;
import net.xqhs.flash.core.support.MessagingPylonProxy;
import net.xqhs.flash.core.support.Pylon;
import net.xqhs.flash.mpi.MPISupport;
import stefania.TreasureHunt.util.Coord;
import static stefania.TreasureHunt.util.Constants.*;

import java.util.ArrayList;
import java.util.List;

public class PlayerAgent implements Agent {
    private String					name;
    private MPISupport.MPIMessaging messagingShard;
    private MessagingPylonProxy pylon;
    private int myRank;
    private int size;
    private Coord oldPos;
    private Coord pos;
    private List<Coord> candidates;

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

    public PlayerAgent(String name, int rank, int size) {
        this.name = name;
        this.myRank = rank;
        this.size = size;
    }

    public void initGame() {
        setPos(new Coord(5, 5));
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
        PlayerInitBehaviour initBehaviour = new PlayerInitBehaviour(this);
        PlayerPlayBehaviour playBehaviour = new PlayerPlayBehaviour(this);
        PlayerEndBehaviour endBehaviour = new PlayerEndBehaviour(this);

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

    public boolean addMessagingShard(MPISupport.MPIMessaging shard)
    {
        messagingShard = shard;
        shard.addContext(proxy);
        if(pylon != null)
            messagingShard.addGeneralContext(pylon);
        return true;
    }

    public static class PlayerInitBehaviour {
        PlayerAgent playerAgent;

        public PlayerInitBehaviour(PlayerAgent playerAgent) {
            this.playerAgent = playerAgent;
        }

        public void action() {
            String hintRequest = "up";

            // Wait tp receive 'START GAME' signal
            playerAgent.messagingShard.receiveMessage(MASTER, PLAYER, "");

            playerAgent.initGame();
            playerAgent.move("up");

            playerAgent.messagingShard.sendMessage(PLAYER, MASTER, hintRequest);
        }
    }

    public static class PlayerPlayBehaviour {
        PlayerAgent playerAgent;
        int nextState;

        public PlayerPlayBehaviour(PlayerAgent playerAgent) {
            this.playerAgent = playerAgent;
            this.nextState = 1;
        }

        public void action() {
            String moveDirection;
            String hint;

            playerAgent.messagingShard.receiveMessage(MASTER, PLAYER, "");
            hint = playerAgent.messagingShard.getMessage();

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

    public static class PlayerEndBehaviour {
        PlayerAgent playerAgent;

        public PlayerEndBehaviour(PlayerAgent playerAgent) {
            this.playerAgent = playerAgent;
        }

        public void action() {
            System.out.println(playerAgent.getName() + "> I found the treasure at " + playerAgent.getPos() + "!");
        }
    }
}

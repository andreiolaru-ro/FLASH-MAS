package test.guiGeneration;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import net.xqhs.flash.core.DeploymentConfiguration;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentEvent.AgentEventType;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.util.MultiTreeMap;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.gui.GuiShard;

public class SimpleGameShard extends AgentShardGeneral {
    /** The serial version ID */
    private static final long serialVersionUID = -8643684908746809219L;
    
    /** The config field name for the other agent */
    private static final String CONFIG_OTHER_AGENT = "otherAgent";
    /** The config field name for the player number */
    private static final String CONFIG_PLAYER_NUMBER = "playerNumber";

    /** The name of the other agent to communicate with */
    private String otherAgent;
    /** Is this agent the first player? */
    private boolean isFirstPlayer;

    /** The timer for the next move */
    private Timer timer = new Timer();
    /** The score of the agent */
    private int score = 0;
    /** The moves made by the opponent */
    private List<Integer> opponentMoves = new ArrayList<>();

    // Inter-agent communication constants
    /** The destination for the opponent's move */
    public static final String MOVE_DEST = "move";
    /** The internal source for playing a move */
    public static final String MOVE_SOURCE = "move";

    // GUI communication constants
    /** The port for the score */
    public static final String SCORE_PORT = "score";
    /** The port for the strategy selection */
    public static final String STRATEGY_PORT = "strategy";
    /** The role for the last opponent move's absolute value */
    public static final String MOVE_ABS_ROLE = "moveAbs";
    /** The role for the sign of the last opponent's move */
    public static final String MOVE_SIGN_ROLE = "moveSign";


    /** A move that the agent can play */
    private interface MoveAction {
        /** Selects the next move based of the last moves of the opponent
         * @return the next move to make, positive to increase, negative to decrease the opponent's score
        */
        public int move();
    }

    /** A strategy for the game */
    private class GameStrategy {
        /** The name of this strategy */
        public String name;
        /** The function to determine the next move based on the opponent's moves */
        public MoveAction nextMove;
        /**
         * Constructor for the GameStrategy.
         * 
         * @param name The name of the strategy.
         * @param nextMove The function that determines the next move.
         */
        public GameStrategy(String name, MoveAction nextMove) {
            this.name = name;
            this.nextMove = nextMove;
        }
    }

    /** The available strategies for the game */
    private GameStrategy[] availableStrategies = {
        new GameStrategy("Always +1", () -> 1),
        new GameStrategy("Always -1", () -> -1),
        new GameStrategy("Random", () -> Math.random() < 0.5 ? 1 : -1),
        new GameStrategy("Counter last", () -> {
            if (opponentMoves.isEmpty()) return 1; // If no moves yet, increase
            Integer lastMove = opponentMoves.get(opponentMoves.size() - 1);
            return lastMove.intValue() > 0 ? -1 : 1; // Counter the last move
        }),
        new GameStrategy("Average", () -> {
            if (opponentMoves.isEmpty()) return 1; // If no moves yet, increase
            int sum = opponentMoves.stream().mapToInt(Integer::intValue).sum();
            return sum / opponentMoves.size();
        }),
        // tit-for-tat strategy
        new GameStrategy("Tit for Tat", () -> {
            if (opponentMoves.isEmpty()) return 1;
            Integer lastMove = opponentMoves.get(opponentMoves.size() - 1);
            return lastMove.intValue();
        })
    };
    /** The current strategy */
    private GameStrategy currentStrategy = availableStrategies[2];

    /**
     * No-argument constructor.
     */
    public SimpleGameShard() {
        super(AgentShardDesignation.autoDesignation("BasicChat"));
    }

    @Override
    public boolean configure(MultiTreeMap configuration) {
        if (!super.configure(configuration))
            return false;
        otherAgent = configuration.getAValue(CONFIG_OTHER_AGENT);
        String playerNumber = configuration.getAValue(CONFIG_PLAYER_NUMBER);
        isFirstPlayer = "1".equals(playerNumber);
        return true;
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        if (event.getType() == AgentEventType.AGENT_START) {
            if (isFirstPlayer) playMove();
        }
        else if (event.getType() == AgentEventType.AGENT_WAVE) {
            AgentWave wave = (AgentWave) event;
            String[] source = wave.getSourceElements();

            if (DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME.equals(source[0])) {
                handleGUIWave(wave);
            } else if (otherAgent.equals(source[0])) {
                handleOpponentWave(wave);
            }
        }
    }

    /**
     * Handles the wave received from the opponent agent.
     * 
     * @param wave The AgentWave containing the opponent's move.
     */
    public void handleOpponentWave(AgentWave wave) {
        GuiShard guiShard = (GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation());
        String dest = wave.getFirstDestinationElement();
        if (!MOVE_DEST.equals(dest)) return;

        int oppMove = 0;
        try { 
            oppMove = Integer.parseInt(wave.getContent());
        } catch (NumberFormatException e) {
            li("Invalid move received from opponent: " + oppMove);
        }
        opponentMoves.add(Integer.valueOf(oppMove));
        score += oppMove;
        
        // Send the updated score to the GUI
        AgentWave scoreWave = new AgentWave(String.valueOf(score), SCORE_PORT);
        scoreWave.add(MOVE_ABS_ROLE, String.valueOf(Math.abs(oppMove)));
        scoreWave.add(MOVE_SIGN_ROLE, (oppMove < 0) ? "-" : "+");
        guiShard.sendOutput(scoreWave);

        playMove();
    }

    /**
     * Plays the next move based on the current strategy.
     * This method is scheduled to run after a delay to simulate a turn-based game.
     */
    public void playMove() {
        timer.schedule(new TimerTask() {
            @SuppressWarnings("synthetic-access")
            @Override
            public void run() {
                int move = currentStrategy.nextMove.move();
                sendMessage(String.valueOf(move), MOVE_SOURCE, otherAgent, MOVE_DEST);
            }
        }, 2000);
    }

    /**
     * Handles the GUI wave for strategy selection.
     * 
     * @param wave The AgentWave containing the strategy selection.
     */
    private void handleGUIWave(AgentWave wave) {
        String[] destination = wave.getDestinationElements();
        if (destination.length < 2) return;

        String port = destination[0];
        String role = destination[1];

        GuiShard guiShard = (GuiShard) getAgentShard(StandardAgentShard.GUI.toAgentShardDesignation());
        if (STRATEGY_PORT.equals(port)) {
            int strategyIndex = Integer.parseInt(role);
            if (strategyIndex < 0 || strategyIndex >= availableStrategies.length) {
                li("Invalid strategy index: " + strategyIndex);
                return;
            }
            currentStrategy = availableStrategies[strategyIndex];
            AgentWave strategyWave = new AgentWave(currentStrategy.name, STRATEGY_PORT);
            guiShard.sendOutput(strategyWave);
        }
    }
}


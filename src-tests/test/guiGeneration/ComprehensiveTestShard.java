package test.guiGeneration;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Random;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.core.support.MessagingShard;
import net.xqhs.flash.core.DeploymentConfiguration;

public class ComprehensiveTestShard extends AgentShardGeneral {
    private static final long serialVersionUID = 1L;
    private boolean reporting = false;
    private Timer timer;
    private Random random = new Random();
    
    private double x = random.nextDouble() * 100;
    private double y = random.nextDouble() * 100;
    private int energy = 50 + random.nextInt(50);
    private String state = "healthy";
    private JsonArray neighbours;

    public ComprehensiveTestShard() {
        super(AgentShardDesignation.autoDesignation("ComprehensiveTest"));
    }

    @Override
    public void signalAgentEvent(AgentEvent event) {
        super.signalAgentEvent(event);
        if (event.getType() == AgentEvent.AgentEventType.AGENT_WAVE) {
            AgentWave wave = (AgentWave) event;
            String[] dest = wave.getDestinationElements();

            boolean isStartCmd = false;
            for (String d : dest) {
                if ("START_REPORTING_METRIC".equals(d)) {
                    isStartCmd = true;
                    break;
                }
            }

            if (isStartCmd && !reporting) {
                reporting = true;
                li("Agent [] start to send data", getAgent().getEntityName());
                startReporting();
            }
        }
    }

    private void startReporting() {
        String myName = getAgent().getEntityName();
		neighbours = new JsonArray();
        
        try {
            int myId = Integer.parseInt(myName.replace("Agent", ""));
            int maxNodes = 20; 
            
            int nextId = (myId % maxNodes) + 1;
			neighbours.add("Agent" + nextId);
            
            if (random.nextDouble() > 0.6) {
                int randomTarget = random.nextInt(maxNodes) + 1;
                if (randomTarget != myId && randomTarget != nextId) {
					neighbours.add("Agent" + randomTarget);
                }
            }
        } catch (Exception e) {
			neighbours.add("Agent1");
        }

        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                x += (random.nextDouble() - 0.5) * 8;
                y += (random.nextDouble() - 0.5) * 8;
                if (x < 0) x = 0; if (x > 100) x = 100;
                if (y < 0) y = 0; if (y > 100) y = 100;

                energy += (random.nextInt(11) - 5);
                if (energy > 100) energy = 100;
                if (energy < 0) energy = 0;

                if (energy < 20) {
                    state = "critical";
                } else if (energy < 60) {
                    state = "warning";
                } else {
                    state = "healthy";
                }

                JsonObject data = new JsonObject();
                data.addProperty("x", x);
                data.addProperty("y", y);
                data.addProperty("energy", energy);
                data.addProperty("state", state);
                data.add("neighbours", neighbours);

                AgentWave reportWave = new AgentWave(data.toString());
                reportWave.resetDestination(DeploymentConfiguration.CENTRAL_MONITORING_ENTITY_NAME, "RECEIVE_METRIC");
                reportWave.addSourceElements(myName);

                MessagingShard messagingShard = (MessagingShard) getAgentShard(
                        AgentShardDesignation.standardShard(AgentShardDesignation.StandardAgentShard.MESSAGING)
                );
                
                if (messagingShard != null) {
                    messagingShard.sendMessage(reportWave);
                }
            }
        }, 0, 1000); 
    }
}

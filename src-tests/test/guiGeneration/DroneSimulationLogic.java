package test.guiGeneration;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonArray;
import net.xqhs.flash.core.agent.AgentEvent;
import net.xqhs.flash.core.shard.AgentShardDesignation;
import net.xqhs.flash.core.shard.AgentShardDesignation.StandardAgentShard;
import net.xqhs.flash.core.shard.AgentShardGeneral;
import net.xqhs.flash.remoteOperation.PropertyContainer;
import net.xqhs.flash.remoteOperation.RemoteOperationShard;

public class DroneSimulationLogic extends AgentShardGeneral implements PropertyContainer {
	// A simple, thread-safe global registry embedded directly inside this shard class
	public static final Map<String, double[]> dronePositions = new ConcurrentHashMap<>();

	private volatile double x, y;
	private volatile double battery = 100.0;
	private volatile String status = "Idle";
	private volatile String color = "#4CAF50";
	private final Random rand = new Random();
	private Timer simulationTimer = null;
	private static final long SIMULATION_STEP_MS = 1000;
	private JsonArray connectedDrones;

	public DroneSimulationLogic() {
		super(AgentShardDesignation.autoDesignation("DroneSimulationLogic"));
		this.x = 200.0 + (rand.nextDouble() * 300.0);
		this.y = 200.0 + (rand.nextDouble() * 300.0);
	}

	public double getX() { return x; }
	public double getY() { return y; }

	@Override
	public void signalAgentEvent(AgentEvent event) {
		super.signalAgentEvent(event);
		switch(event.getType()) {
			case AGENT_START:
				buildNeighbours();
				startSimulation();

				RemoteOperationShard remoteOp = (RemoteOperationShard) getAgent().getAgentShard(StandardAgentShard.REMOTE.toAgentShardDesignation());
				if (remoteOp != null) {
					Set<String> properties = new HashSet<>();
					properties.add("longitude");
					properties.add("latitude");
					properties.add("battery");
					properties.add("status");
					properties.add("color");
					properties.add("connectedDrones");
					remoteOp.registerOutputProperties(this, properties);
				}
				break;
			case AGENT_STOP:
				if(simulationTimer != null) {
					simulationTimer.cancel();
					simulationTimer = null;
				}
				// Clean up position registry when this drone stops
				dronePositions.remove(getAgent().getEntityName());
				break;
			default:
				break;
		}
	}

	private void buildNeighbours() {
		connectedDrones = new JsonArray();
		int myNum = Integer.parseInt(getAgent().getEntityName().split("_")[1]);
		if (myNum > 1) connectedDrones.add("Drone_" + (myNum - 1));
		if (rand.nextDouble() < 0.15 && myNum < 249) {
			connectedDrones.add("Drone_" + (myNum + 1));
		}
	}

	private void startSimulation() {
		simulationTimer = new Timer(true);
		simulationTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				if (battery < 20.0 && !status.equals("Charging")) {
					status = "LowBattery";
					color = "#F44336";
				} else if (status.equals("Charging") && battery >= 95.0) {
					status = "Idle";
					color = "#4CAF50";
				} else if (status.equals("Idle") && rand.nextDouble() < 0.3) {
					status = "Delivering";
					color = "#2196F3";
				}

				if (status.equals("Delivering")) {
					x += (rand.nextDouble() - 0.5) * 25.0;
					y += (rand.nextDouble() - 0.5) * 25.0;
					battery -= 1.5 + (rand.nextDouble() * 0.5);
				} else if (status.equals("LowBattery")) {
					x += (x > 350.0) ? -15.0 : 15.0;
					y += (y > 350.0) ? -15.0 : 15.0;
					battery -= 0.5;
					if (Math.abs(x - 350.0) < 30.0 && Math.abs(y - 350.0) < 30.0) {
						status = "Charging";
						color = "#FF9800";
					}
				} else if (status.equals("Charging")) {
					battery += 8.0;
				} else {
					battery -= 0.1;
				}
				if (battery < 0) battery = 0;

				// Keep drone within boundaries to prevent sector index overflow errors
				if (x < 0.0) x = 0.0;
				if (x >= 500.0) x = 499.9;
				if (y < 0.0) y = 0.0;
				if (y >= 500.0) y = 499.9;

				// Save the current position to the shared map
				dronePositions.put(getAgent().getEntityName(), new double[]{x, y});
			}
		}, 0, SIMULATION_STEP_MS);
	}

	@Override
	public Map<String, String> getProperties(Set<String> properties) {
		Map<String, String> metrics = new HashMap<>();
		metrics.put("longitude", String.valueOf(x));
		metrics.put("latitude", String.valueOf(y));
		metrics.put("battery", String.valueOf(battery));
		metrics.put("status", status);
		metrics.put("color", color);
		if (connectedDrones != null) {
			metrics.put("connectedDrones", connectedDrones.toString());
		}
		return metrics;
	}
}
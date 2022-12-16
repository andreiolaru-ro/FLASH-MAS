package JadeScript;

import java.util.ArrayList;
import java.util.List;

import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.leap.Properties;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import testing.TimeMonitor;

public class Launcher {
	AgentContainer			container;
	List<AgentController>	agentCtrl	= new ArrayList<>();
	
	AgentContainer setupPlatform(String mainHost, int mainPort, String host, int port, int index, boolean isMain) {
		Properties mainProps = new ExtendedProperties();
		if(isMain) {
			mainProps.setProperty(Profile.GUI, "true"); // start the JADE GUI
			mainProps.setProperty(Profile.MAIN, "true"); // is main container
		}
		mainProps.setProperty(Profile.CONTAINER_NAME, "Container-" + index); // you can rename it
		// TODO: replace with actual IP of the current machine
		mainProps.setProperty(Profile.LOCAL_HOST, host);
		mainProps.setProperty(Profile.LOCAL_PORT, Integer.valueOf(port).toString());
		mainProps.setProperty(Profile.MAIN_HOST, mainHost);
		mainProps.setProperty(Profile.MAIN_PORT, Integer.valueOf(mainPort).toString());
		
		ProfileImpl mainProfile = new ProfileImpl(mainProps);
		container = isMain ? Runtime.instance().createMainContainer(mainProfile)
				: Runtime.instance().createAgentContainer(mainProfile);
		return container;
	}
	
	AgentController addAgent(String name, String cls, Object[] args) {
		AgentController ag;
		try {
			ag = container.createNewAgent(name, cls, args);
			agentCtrl.add(ag);
			return ag;
		} catch(StaleProxyException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Starts the agents assigned to the main container.
	 */
	void startAgents() {
		try {
			for(AgentController ag : agentCtrl)
				ag.start();
		} catch(StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	public static Launcher[] script1(int index) {
		String[] names = { "A", "B", "C", "D" };
		String[] server = { "172.19.3.92", "172.19.3.50", "172.19.3.132", "172.19.3.206" };
		String base = index < 0 ? "localhost" : server[0];
		
		Launcher[] launcher = new Launcher[index < 0 ? 4 : 1];
		
		String srv = index < 0 ? base : server[index];
		int j = 0;
		for(int i = index < 0 ? 0 : index; i < (index < 0 ? launcher.length : index + 1); i++) {
			launcher[j] = new Launcher();
			launcher[j].setupPlatform(base, 1099, srv, 1099 + (index < 0 ? i : 0), index < 0 ? i : index, i == 0);
			launcher[j].addAgent(names[i], i == 0 ? MobileAgent.class.getName() : MessagingAgent.class.getName(),
					new Object[] { "A", "51" });
			j++;
		}
		return launcher;
	}
	
	public static Launcher[] script2(int index) {
		String[] server = { "172.19.3.92", "172.19.3.50", "172.19.3.132", "172.19.3.206" };
		String base = index < 0 ? "localhost" : server[0];
		
		Launcher[] launcher = new Launcher[index < 0 ? 4 : 1];
		
		for(int i = 0; i < launcher.length; i++) {
			String srv = index < 0 ? base : server[index];
			launcher[i] = new Launcher();
			launcher[i].setupPlatform(base, 1099, srv, 1099 + (index < 0 ? i : 0), i, i == 0);
			
			launcher[i].addAgent(Integer.valueOf(4 * i).toString(), MessagingAgent.class.getName(),
					new Object[] { Integer.valueOf((4 * i + 8 + 1) % 16).toString() });
			launcher[i].addAgent(Integer.valueOf(4 * i + 1).toString(), PongAgent.class.getName(), null);
			launcher[i].addAgent(Integer.valueOf(4 * i + 2).toString(), MessagingAgent.class.getName(),
					new Object[] { Integer.valueOf((4 * i + 2 + 8 + 1) % 16).toString() });
			launcher[i].addAgent(Integer.valueOf(4 * i + 3).toString(), PongAgent.class.getName(), null);
		}
		return launcher;
	}
	
	/**
	 * Launches the main container.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		
		int index = -1;
		int script = 1;
		if(args.length > 0)
			script = Integer.parseInt(args[0]);
		if(args.length > 1)
			index = Integer.parseInt(args[1]);
		
		Launcher[] launchers;
		switch(script) {
		case 2:
			launchers = script2(index);
			break;
		case 1:
		default:
			launchers = script1(index);
			break;
		}
		
		TimeMonitor time = new TimeMonitor();
		time.start();
		
		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < launchers.length; i++) {
			launchers[i].startAgents();
		}
	}
}

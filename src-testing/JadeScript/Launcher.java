package JadeScript;

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
	AgentContainer	container;
	AgentController	agentCtrl;
	
	void setupPlatform(String mainHost, int mainPort, String host, int port, int index, boolean isMain, String agent) {
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
		try {
			agentCtrl = container.createNewAgent(agent,
					isMain ? MobileAgent.class.getName() : MessagingAgent.class.getName(), null);
		} catch(StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts the agents assigned to the main container.
	 */
	void startAgents() {
		try {
			agentCtrl.start();
		} catch(StaleProxyException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Launches the main container.
	 * 
	 * @param args
	 *            - not used.
	 */
	public static void main(String[] args) {
		
		String[] names = { "A", "B", "C", "D" };
		String[] server = { "localhost" };
		Launcher[] launcher = new Launcher[4];
		
		// int i = Integer.parseInt(args[0]);
		
		for(int i = 0; i < 4; i++) {
			String srv = server[0];
			launcher[i] = new Launcher();
			launcher[i].setupPlatform(server[0], 1099, srv, 1099 + i, i, i == 0, names[i]);
		}
		TimeMonitor time = new TimeMonitor();
		time.start();
		
		try {
			Thread.sleep(1000);
		} catch(InterruptedException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < 4; i++) {
			launcher[i].startAgents();
		}
	}
}

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
		String[] server = { "172.19.3.92", "172.19.3.132", "172.19.3.50", "172.19.3.206" };
		String base = index < 0 ? "localhost" : server[0];
		
		Launcher[] launcher = new Launcher[index < 0 ? 4 : 1];
		
		String srv = index < 0 ? base : server[index];
		int j = 0;
		for(int i = index < 0 ? 0 : index; i < (index < 0 ? launcher.length : index + 1); i++) {
			launcher[j] = new Launcher();
			launcher[j].setupPlatform(base, 1099, srv, 1099 + (index < 0 ? i : 0), index < 0 ? i : index, i == 0);
			// make additional agents to avoid linux hosts where agent instantiation fails
			for(int k : new Integer[] { 0, 1 }) // -1 for index = 3
				launcher[j].addAgent(names[i + k],
						i + k == 0 ? MobileAgent.class.getName() : MessagingAgent.class.getName(),
					new Object[] { "A", "51" });
			j++;
		}
		return launcher;
	}
	
	public static Launcher[] script2(int index) {
		String[] server = { "172.19.3.92", "172.19.3.132", "172.19.3.50", "172.19.3.206" };
		String base = index < 0 ? "localhost" : server[0];
		
		Launcher[] launcher = new Launcher[index < 0 ? 4 : 1];
		
		String srv = index < 0 ? base : server[index];
		int j = 0;
		for(int i = index < 0 ? 0 : index; i < (index < 0 ? launcher.length : index + 1); i++) {
			launcher[j] = new Launcher();
			launcher[j].setupPlatform(base, 1099, srv, 1099 + (index < 0 ? i : 0), index < 0 ? i : index, i == 0);
			
			launcher[j].addAgent(Integer.valueOf(4 * i + 2).toString(), PongAgent.class.getName(), null);
			launcher[j].addAgent(Integer.valueOf(4 * i + 4).toString(), PongAgent.class.getName(), null);
			
			int correspondent1, correspondent2;
			switch(i) {
			case 0:
			default:
				// node 1
				correspondent1 = 8;
				correspondent2 = 12;
				break;
			case 1:
				// node 2
				correspondent1 = 14;
				correspondent2 = 2;
				break;
			case 2:
				// node 3
				correspondent1 = 16;
				correspondent2 = 4;
				break;
			case 3:
				// node 4
				correspondent1 = 6;
				correspondent2 = 10;
				break;
			}
			
			launcher[j].addAgent(Integer.valueOf(4 * i + 1).toString(), MessagingAgent.class.getName(),
					new Object[] { Integer.valueOf(correspondent1).toString(), Integer.valueOf(101).toString() });
			launcher[j].addAgent(Integer.valueOf(4 * i + 3).toString(), MessagingAgent.class.getName(),
					new Object[] { Integer.valueOf(correspondent2).toString(), Integer.valueOf(101).toString() });
			j++;
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
		int script = 2;
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

package JadeScript;

import jade.core.Agent;
import jade.core.ContainerID;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.lang.acl.ACLMessage;
import testing.TimeMonitor;

public class MobileAgent extends Agent {
	int	cycle	= 0;
	int	cnode	= 0;
	
	@Override
	protected void setup() {
		super.setup();
		
		addBehaviour(new CyclicBehaviour() {
			
			@Override
			public void action() {
				ACLMessage msg = receive();
				if(msg == null) {
					block();
					return;
				}
				System.out.println(getLocalName() + " Received: " + msg.getContent() + " from " + msg.getSender());
				ACLMessage reply = msg.createReply();
				reply.setContent(msg.getContent() + " reply");
				myAgent.send(reply);
			}
		});
		TimeMonitor.markTime("A start");
		cnode = 1;
		System.out.println("moving to " + cnode);
		doMove(new ContainerID("Container-1", null));
	}
	
	@Override
	protected void afterMove() {
		super.afterMove();
		if(cycle < 4)
			// addBehaviour(new WakerBehaviour(this, 20) {
			addBehaviour(new OneShotBehaviour() {
				@Override
				// public void onWake() {
				public void action() {
					cnode += 1;
					cnode = cnode % 4;
					if(cnode == 0)
						cycle += 1;
					System.out.println("moving to " + cnode);
					doMove(new ContainerID("Container-" + cnode, null));
				}
			});
		else
			TimeMonitor.markTime("A DONE");
	}
}

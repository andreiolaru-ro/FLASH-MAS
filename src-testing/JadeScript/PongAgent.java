package JadeScript;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class PongAgent extends Agent {
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
				System.out.println(getLocalName() + " Received: [" + msg.getContent() + "] from " + msg.getSender());
				ACLMessage reply = msg.createReply();
				reply.setContent(msg.getContent() + "    reply");
				myAgent.send(reply);
			}
		});
	}
}

package JadeScript;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import testing.TimeMonitor;

public class MessagingAgent extends Agent {
	@Override
	protected void setup() {
		super.setup();
		
		AID destination = new AID((String) getArguments()[0], AID.ISLOCALNAME);
		
		TimeMonitor.markTime(getLocalName() + " start");
		ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
		msg1.addReceiver(destination);
		msg1.setContent(getLocalName() + "  00");
		send(msg1);
		
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
				int index = Integer.parseInt(msg.getContent().substring(2, 6).trim()) + 1;
				if(index <= 51) {
					reply.setContent(getLocalName() + "  " + String.format("%2d", index).replace(' ', '0'));
					myAgent.send(reply);
				}
				else
					TimeMonitor.markTime(getLocalName() + " DONE");
			}
		});
	}
}

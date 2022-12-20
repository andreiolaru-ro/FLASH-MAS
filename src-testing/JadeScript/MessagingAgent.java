package JadeScript;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.lang.acl.ACLMessage;
import testing.TimeMonitor;

public class MessagingAgent extends Agent {
	@Override
	protected void setup() {
		super.setup();
		
		AID destination = new AID((String) getArguments()[0], AID.ISLOCALNAME);
		int nMsgs = Integer.parseInt((String) getArguments()[1]);
		
		if(getAID().getLocalName().equals("1")) {
			System.out.println("This will start");
			addBehaviour(new WakerBehaviour(this, 1000) {
				@Override
				protected void onWake() {
					super.onWake();
					TimeMonitor.markTime(getLocalName() + " boot");
					ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
					for(int i = 3; i < 17; i += 2)
						msg1.addReceiver(new AID(Integer.valueOf(i).toString(), AID.ISLOCALNAME));
					// variant for LauncherA
					// for(int i = 2; i <= 4; i += 1)
					// msg1.addReceiver(new AID(Integer.valueOf(i).toString(), AID.ISLOCALNAME));
					// for(int i = 13; i <= 16; i += 1)
					// msg1.addReceiver(new AID(Integer.valueOf(i).toString(), AID.ISLOCALNAME));
					// msg1.setContent("start");
					send(msg1);
				}
				
			});
		}
		
		addBehaviour(new CyclicBehaviour() {
			boolean first = true;
			
			@Override
			public void action() {
				ACLMessage msg = receive();
				if(msg == null) {
					block();
					return;
				}
				if(first) {
					first = false;
					addBehaviour(new WakerBehaviour(myAgent, 5000) {
						@Override
						protected void onWake() {
							super.onWake();
							TimeMonitor.markTime(getLocalName() + " start");
							ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
							msg1.addReceiver(destination);
							msg1.setContent(getLocalName() + "  00");
							send(msg1);
						}
					});
				}
				else {
					System.out
							.println(getLocalName() + " Received: [" + msg.getContent() + "] from " + msg.getSender());
					ACLMessage reply = msg.createReply();
					int index = Integer.parseInt(msg.getContent().substring(2, 8).trim()) + 1;
					if(index <= nMsgs) {
						reply.setContent(getLocalName() + "  " + String.format("%2d", index).replace(' ', '0'));
						myAgent.send(reply);
					}
					else
						TimeMonitor.markTime(getLocalName() + " DONE");
				}
			}
		});
	}
}

package JadeScript;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class MessagingAgent extends Agent {
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
				reply.setContent(msg.getContent() + "   reply");
				myAgent.send(reply);
			}
		});
	}
	
	// boolean first = true;
	// @Override
	// protected void setup() {
	// super.setup();
	//
	// System.out.println("Setup");
	// int nMsgs = Integer.parseInt((String) getArguments()[1]);
	// System.out.println("Arg - n: " + nMsgs);
	// AID destination = new AID((String) getArguments()[0], AID.ISLOCALNAME);
	// System.out.println("Args - dest:" + destination);
	//
	// // if(getAID().getLocalName().equals("1")) {
	// // System.out.println("This will start");
	// // addBehaviour(new WakerBehaviour(this, 15000) {
	// // @Override
	// // protected void onWake() {
	// // super.onWake();
	// // TimeMonitor.markTime(getLocalName() + " boot");
	// // ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
	// // for(int i = 3; i < 17; i += 2)
	// // msg1.addReceiver(new AID(Integer.valueOf(i).toString(), AID.ISLOCALNAME));
	// // msg1.setContent("start");
	// // send(msg1);
	// // }
	// //
	// // });
	// // }
	//
	// addBehaviour(new CyclicBehaviour() {
	//
	// @Override
	// public void action() {
	// ACLMessage msg = receive();
	// if(msg == null) {
	// block();
	// return;
	// }
	// // if(first) {
	// // first = false;
	// // addBehaviour(new WakerBehaviour(myAgent, 5000) {
	// // @Override
	// // protected void onWake() {
	// // super.onWake();
	// // // TimeMonitor.markTime(getLocalName() + " start");
	// // ACLMessage msg1 = new ACLMessage(ACLMessage.INFORM);
	// // msg1.addReceiver(destination);
	// // msg1.setContent(getLocalName() + " 00");
	// // send(msg1);
	// // }
	// // });
	// // }
	// // else {
	// // System.out
	// // .println(getLocalName() + " Received: [" + msg.getContent() + "] from " + msg.getSender());
	// // ACLMessage reply = msg.createReply();
	// // int index = Integer.parseInt(msg.getContent().substring(2, 8).trim()) + 1;
	// // if(index <= nMsgs) {
	// // reply.setContent(getLocalName() + " " + String.format("%2d", index).replace(' ', '0'));
	// // myAgent.send(reply);
	// // }
	// // else
	// // TimeMonitor.markTime(getLocalName() + " DONE");
	// // }
	// }
	// });
	// }
}

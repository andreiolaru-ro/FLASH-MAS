import net.xqhs.flash.core.util.MultiTreeMap;

import java.util.Map;

import net.xqhs.flash.core.Operation;
import net.xqhs.flash.core.composite.CompositeAgent;

public class Relation {
	static enum RelationChangeType {
		CREATE, DESTROY
	};
	
	EntityID getFrom();
	
	EntityID getTo();
	
	String getRelation();
}

public interface EntityAPI {
	start, stop, isRunning, 
	
	setup, getEntityTools;
	
	handleRelationChange(Map<RelationChangeType, Relation> changes);
}

public interface AgentModel {
	public Operation RECEIVE = new Operation() {
		
	}
	
	receiveMessage(sourceAgentID, content);
}

public interface ArtifactModel {
	
}

public interface ShardModel {
	public static SHARD_MODEL_NAME = "shard";
}

public abstract class BasicAgent extends BasicEntity implements AgentModel {
	EntityTools		support;
	MultiTreeMap	configuration;
	
	setup(EntityTools tools) {
		support = tools;
		configuration = support.getConfiguration();
	}
	
	sendMessage(destination, content) {
		support.handleCall(destinationAgentID, AgentModel.RECEIVE, content);
	}
}
	
	public interface EntityTools {
	initialize(entityName);
		
		getOperationList();getOperation(Operation);createOperation(Operation);removeOperation(Operation);
		
		getRelations();getIncomingRelations(); // children
		getOutgoingRelations(); // parents
		
	handleCall(String destinationEntityID, Operation operation, Object... args);
	
	handleCall(OperationCall call);
	}
	
	public class MyNonCompositeAgent implements EntityScenarioModel extends BasicAgent {
		
		public MyNonCompositeAgent() {
		}
		
	setup(EntityTools tools) {
		tools.initialize(name); // if entity does not perform initialization, initialize will be called after setup
		// registers with the framework
		
		// all existing relations are currently in tools
		tools.createOperation(EntityScenarioModel.SCENARIO_START).setCallBack(this);
		
		// advanced message matching: add individual sub-entities (e.g. behaviors or shards); or use argument patterns 
	}
		
	@Override
	start() {
		...
		// send a message to another agent
		sendMessage(destinationAgentID, content);
		// instead of
		support.handleCall(destinationAgentID, AgentModel.RECEIVE, content);
	}
		
	@Override
	receiveMessage(FIPAACLMessage message) {
		// match the message, contact an artifact to get a result and respond to the message
		if(argumentmatcher.matches(message))	{
			// could be:
			
			// blocking, OO-approach
			SpecificArtifactStub art = new SpecificArtifactStub(support.getStubSupport(), artifact_addr);
			// support auto-selects necessary auth tokens
			Integer result = art.compute(arg1, arg2); // blocking call implemented by stub
			
			// blocking, with general stub
			ArtifactStub art = new ArtifactStub(support.getStubSupport(), artifact_addr);
			// support auto-selects necessary auth tokens
			Object result = art.invokeOperationBlocking("compute", arg1, arg2);
			
			// non-blocking, with stub
			ArtifactStub art = new ArtifactStub(support.getStubSupport(), artifact_addr).prepareOp("compute");
			// gets the description of the <compute> operation asynchronuously
			// support auto-selects necessary auth tokens
			art.invokeOperationWithResult("compute", arg1, arg2, new ResultRecevier() {receive(Object result) {}} );
			
			// non-blocking, entity-op
			support.handleCallWithResult(artifact_addr, "compute", arg, arg2, tokens, new ResultRecevier() {receive(Object result) {}} );
			
			sendMessage(message.createReply().setContent());
		}
	}
}
		
public class MyCompositeAgent extends CompositeAgent implements AgentModel {
	setup(EntityTools tools) {
		tools.initialize(name);
		// TODO
		
		KnowledgeShardModel kshard = tools.createSubEntity(KnowledgeShardModel.KNOWLEDGE_SHARD_MODEL_NAME, KnowledgeShardModel.class); // force conversion
		
		// or
		
		tools.createSubEntity(classPath, name, new Relation().setFrom(this.getName()).setRelation("composes"), args);
	}
}



















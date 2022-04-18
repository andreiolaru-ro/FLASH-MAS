package net.xqhs.flash.ent_op.model;

import java.util.ArrayList;
import java.util.Set;

public class OperationCall {
	public interface AuthorizationToken {
	}

	private EntityID				sourceEntity;
	private EntityID				targetEntity;
	private String					targetOperation;
	private boolean					sendReturnValue;
	private boolean                 routed;
	private ArrayList<Object>		argumentValues;
	private Set<AuthorizationToken>	tokens;

	public OperationCall(EntityID sourceEntity, EntityID targetEntity, String targetOperation, boolean sendReturnValue, ArrayList<Object> argumentValues) {
		this.sourceEntity = sourceEntity;
		this.targetEntity = targetEntity;
		this.targetOperation = targetOperation;
		this.sendReturnValue = sendReturnValue;
		this.argumentValues = argumentValues;
	}

	public OperationCall(String targetOperation) {
		this.targetOperation = targetOperation;
	}
	
	public void setArguments(Object... args) {
		// error if number or type of arguments is incorrect
		// TODO
	}
	
	public void addToken(AuthorizationToken token) {
		// TODO
	}

	public EntityID getSourceEntity() {
		return sourceEntity;
	}

	public String getOperationName() {
		return targetOperation;
	}

	public boolean wasRouted() {
		return routed;
	}

	public void setRouted(boolean routed) {
		this.routed = routed;
	}

	public EntityID getTargetEntity() {
		return targetEntity;
	}

	public void setTargetEntity(EntityID targetEntity) {
		this.targetEntity = targetEntity;
	}

	public ArrayList<Object> getArgumentValues() {
		return argumentValues;
	}
}
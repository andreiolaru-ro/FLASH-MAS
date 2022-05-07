package net.xqhs.flash.ent_op.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Set;

public class OperationCall implements Serializable {
	public interface AuthorizationToken {
	}

	private EntityID				sourceEntity;
	private EntityID				targetEntity;
	private String					targetOperation;
	private boolean					sendReturnValue;
	private boolean                 routed;
	private ArrayList<Object>		argumentValues;
	private Set<AuthorizationToken>	tokens;

	public OperationCall() {

	}

	public OperationCall(String targetOperation) {
		this.targetOperation = targetOperation;
	}

	public OperationCall(EntityID sourceEntity, EntityID targetEntity, String targetOperation, boolean sendReturnValue, ArrayList<Object> argumentValues) {
		this.sourceEntity = sourceEntity;
		this.targetEntity = targetEntity;
		this.targetOperation = targetOperation;
		this.sendReturnValue = sendReturnValue;
		this.argumentValues = argumentValues;
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

	public void setSourceEntity(EntityID sourceEntity) {
		this.sourceEntity = sourceEntity;
	}

	public EntityID getTargetEntity() {
		return targetEntity;
	}

	public void setTargetEntity(EntityID targetEntity) {
		this.targetEntity = targetEntity;
	}

	public String getTargetOperation() {
		return targetOperation;
	}

	public void setTargetOperation(String targetOperation) {
		this.targetOperation = targetOperation;
	}

	public boolean isSendReturnValue() {
		return sendReturnValue;
	}

	public void setSendReturnValue(boolean sendReturnValue) {
		this.sendReturnValue = sendReturnValue;
	}

	public boolean isRouted() {
		return routed;
	}

	public void setRouted(boolean routed) {
		this.routed = routed;
	}

	public ArrayList<Object> getArgumentValues() {
		return argumentValues;
	}

	public void setArgumentValues(ArrayList<Object> argumentValues) {
		this.argumentValues = argumentValues;
	}

	public Set<AuthorizationToken> getTokens() {
		return tokens;
	}

	public void setTokens(Set<AuthorizationToken> tokens) {
		this.tokens = tokens;
	}
}
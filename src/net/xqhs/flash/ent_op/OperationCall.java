package net.xqhs.flash.ent_op;

import java.util.ArrayList;
import java.util.Set;

public class OperationCall {
	public interface AuthorizationToken {
	}

	EntityID				sourceEntity;
	EntityID				targetEntity;
	String					targetOperation;
	boolean					sendReturnValue;
	ArrayList<Object>		argumentValues;
	Set<AuthorizationToken>	tokens;
	
	public OperationCall(String operationName) {
		// TODO Auto-generated constructor stub
	}
	
	public void setArguments(Object... args) {
		// error if number or type of arguments is incorrect
		// TODO
	}
	
	public void addToken(AuthorizationToken token) {
		// TODO
	}
}
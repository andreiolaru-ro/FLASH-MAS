package net.xqhs.flash.ent_op.impl.waves;

import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Wave;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static net.xqhs.flash.ent_op.model.Wave.WaveType.OPERATION_CALL;

public class OperationCallWave extends Wave implements Serializable {
    public interface AuthorizationToken {
    }

    protected String id;
    private String targetOperation;
    private boolean sendReturnValue;
    private List<Object> argumentValues;
    private Set<AuthorizationToken> tokens;

    private Object result;

    public OperationCallWave() {

    }

    public OperationCallWave(EntityID sourceEntity, EntityID targetEntity, String targetOperation, boolean sendReturnValue, List<Object> argumentValues) {
        // TODO implement an id generator
        this.id = targetOperation;
        this.type = OPERATION_CALL;
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

    public List<Object> getArgumentValues() {
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

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
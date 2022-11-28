package net.xqhs.flash.ent_op.impl.waves;

import net.xqhs.flash.ent_op.model.EntityID;
import net.xqhs.flash.ent_op.model.Wave;

import java.io.Serializable;

import static net.xqhs.flash.ent_op.model.Wave.WaveType.RESULT;

public class ResultWave extends Wave implements Serializable {

    protected String operationCallId;
    protected Object result;

    public ResultWave() {

    }

    public ResultWave(EntityID sourceEntity, EntityID targetEntity, String operationCallId, Object result) {
        this.type = RESULT;
        this.sourceEntity = sourceEntity;
        this.targetEntity = targetEntity;
        this.operationCallId = operationCallId;
        this.result = result;
    }

    public String getOperationCallId() {
        return operationCallId;
    }

    public void setOperationCallId(String operationCallId) {
        this.operationCallId = operationCallId;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}

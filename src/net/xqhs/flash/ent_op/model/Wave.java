package net.xqhs.flash.ent_op.model;

public abstract class Wave {
    public enum WaveType {
        OPERATION_CALL, RELATION_CHANGE, RESULT
    }

    protected String id;
    protected EntityID sourceEntity;
    protected EntityID targetEntity;
    protected boolean routed;
    protected WaveType type;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public boolean isRouted() {
        return routed;
    }

    public void setRouted(boolean routed) {
        this.routed = routed;
    }

    public WaveType getType() {
        return type;
    }

    public void setType(WaveType type) {
        this.type = type;
    }
}

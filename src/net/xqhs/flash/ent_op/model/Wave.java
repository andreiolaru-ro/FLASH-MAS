package net.xqhs.flash.ent_op.model;

public abstract class Wave {
    public enum WaveType {
        OPERATION_CALL, RELATION_CHANGE, RESULT, RELATION_CHANGE_RESULT
    }

    protected String id;
    protected EntityID sourceEntity;
    protected EntityID targetEntity;
	@Deprecated
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

	@Deprecated
    public boolean isRouted() {
        return routed;
    }

	@Deprecated
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

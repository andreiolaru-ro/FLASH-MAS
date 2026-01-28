package net.xqhs.flash.core.recorder;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * Data Transfer Object (DTO) representing a single recorded event in the simulation timeline.
 * <p>
 * Used for serialization by Gson. Contains the timestamp, source entity,
 * event type, and any associated payload.
 * </p>
 */
public class SimulationEvent implements Serializable {
    protected final long timestamp;
    protected final String entityName;
    protected final String type;
    protected final Object payload;

    protected static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
            .withZone(ZoneId.systemDefault());

    public SimulationEvent(String entityName, String type, Object payload) {
        this.timestamp = System.currentTimeMillis();
        this.entityName = entityName;
        this.type = type;
        this.payload = payload;
    }

    @Override
    public String toString() {
        String humanTime = TIME_FORMATTER.format(Instant.ofEpochMilli(timestamp));
        String payloadString;

        if (payload instanceof Object[]) {
            payloadString = Arrays.deepToString((Object[]) payload);
        } else {
            payloadString = String.valueOf(payload);
        }

        return String.format("Event[%s] %s: %s -> %s",
                humanTime,
                entityName,
                type,
                payloadString
        );
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getType() {
        return type;
    }

    public Object getPayload() {
        return payload;
    }
}
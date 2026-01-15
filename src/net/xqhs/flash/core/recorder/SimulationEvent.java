package net.xqhs.flash.core.recorder;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

public class SimulationEvent implements Serializable {
    public final long timestamp;
    public final String entityName;
    public final String type;
    public final Object payload;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
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
}
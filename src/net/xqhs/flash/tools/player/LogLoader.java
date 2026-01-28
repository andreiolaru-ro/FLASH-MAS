package net.xqhs.flash.tools.player;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.xqhs.flash.core.recorder.SimulationEvent;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Utility class designed to handle file I/O operations for simulation logs.
 * <p>
 * Uses Gson to parse the JSON array into a list of {@link SimulationEvent} objects.
 * Ensures strict chronological sorting of events based on timestamps.
 * </p>
 */
public class LogLoader {

    public static List<SimulationEvent> loadFromFile(String filePath) throws IOException {
        Gson gson = new Gson();

        try (Reader reader = new FileReader(filePath)) {
            Type listType = new TypeToken<ArrayList<SimulationEvent>>(){}.getType();

            List<SimulationEvent> events = gson.fromJson(reader, listType);

            if (events == null) {
                return new ArrayList<>();
            }

            Collections.sort(events, Comparator.comparingLong(o -> o.getTimestamp()));

            System.out.println("[LogLoader] Loaded " + events.size() + " events from " + filePath);
            return events;
        }
    }
}
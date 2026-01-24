package net.xqhs.flash.ml;

import net.xqhs.flash.core.agent.AgentWave;
import net.xqhs.flash.core.support.WaveReceiver;

/**
 * Interface for entities that can execute tasks asynchronously.
 * Supports callback-based async patterns.
 */
public interface AsyncDriver {

    /**
     * Receives a wave and processes it asynchronously using a callback.
     * The callback will be invoked when processing completes.
     *
     * @param wave The input wave containing request data
     * @param callback The receiver to call with the reply wave
     */
    void processAsync(AgentWave wave, WaveReceiver callback);

    AgentWave process(AgentWave wave);
}

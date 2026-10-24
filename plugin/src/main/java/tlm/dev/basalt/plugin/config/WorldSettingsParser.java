package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.world.WorldSettings;

public final class WorldSettingsParser {

    public static final String MAX_CHUNKS = "world.max-chunks";
    public static final String MAX_WORLDS = "world.max-worlds";
    public static final String MAX_CHUNK_SENDS_PER_TICK = "world.max-chunk-sends-per-tick";
    public static final String ENCODER_THREADS = "world.encoder-threads";
    public static final int SENDS_PER_TICK_LIMIT = 4096;
    public static final int ENCODER_THREADS_LIMIT = 16;

    private WorldSettingsParser() {
    }

    public static WorldSettings parse(ConfigReader config) {
        return new WorldSettings(
                config.wholeNumber(MAX_CHUNKS, 1, WorldSettings.MAX_CHUNKS_LIMIT),
                config.wholeNumber(MAX_WORLDS, 1, WorldSettings.MAX_WORLDS_LIMIT));
    }

    public static int chunkSendsPerTick(ConfigReader config) {
        return config.wholeNumber(MAX_CHUNK_SENDS_PER_TICK, 1, SENDS_PER_TICK_LIMIT);
    }

    public static int encoderThreads(ConfigReader config) {
        return config.wholeNumber(ENCODER_THREADS, 1, ENCODER_THREADS_LIMIT);
    }
}

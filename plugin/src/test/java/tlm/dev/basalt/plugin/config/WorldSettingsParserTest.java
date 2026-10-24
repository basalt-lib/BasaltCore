package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.world.WorldSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSettingsParserTest {

    private static final String PATH = "plugins/BasaltCore/config.yml";

    private final List<String> warnings = new ArrayList<>();

    @Test
    void shipsTheWorldLimits() throws IOException {
        assertEquals("256", ShippedConfig.entries().get(WorldSettingsParser.MAX_CHUNKS));
        assertEquals("64", ShippedConfig.entries().get(WorldSettingsParser.MAX_WORLDS));
        assertEquals("16", ShippedConfig.entries().get(WorldSettingsParser.MAX_CHUNK_SENDS_PER_TICK));
        assertEquals(new WorldSettings(256, 64), WorldSettingsParser.parse(reader(Map.of())));
        assertEquals(16, WorldSettingsParser.chunkSendsPerTick(reader(Map.of())));
        assertEquals("2", ShippedConfig.entries().get(WorldSettingsParser.ENCODER_THREADS));
        assertEquals(2, WorldSettingsParser.encoderThreads(reader(Map.of())));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheServerValuesAndRefusesBadOnes() throws IOException {
        assertEquals(new WorldSettings(4, 2), WorldSettingsParser.parse(reader(Map.of(
                WorldSettingsParser.MAX_CHUNKS, 4, WorldSettingsParser.MAX_WORLDS, 2))));
        assertEquals(new WorldSettings(256, 64), WorldSettingsParser.parse(reader(Map.of(
                WorldSettingsParser.MAX_CHUNKS, 0, WorldSettingsParser.MAX_WORLDS, "many"))));
        assertEquals(1024, WorldSettingsParser.chunkSendsPerTick(reader(Map.of(WorldSettingsParser.MAX_CHUNK_SENDS_PER_TICK, 1024))));
        assertEquals(16, WorldSettingsParser.chunkSendsPerTick(reader(Map.of(WorldSettingsParser.MAX_CHUNK_SENDS_PER_TICK, -3))));
        assertEquals(4, WorldSettingsParser.encoderThreads(reader(Map.of(WorldSettingsParser.ENCODER_THREADS, 4))));
        assertEquals(2, WorldSettingsParser.encoderThreads(reader(Map.of(WorldSettingsParser.ENCODER_THREADS, 17))));

        assertEquals(List.of(
                PATH + ": world.max-chunks must be a whole number from 1 to 4096, using 256",
                PATH + ": world.max-worlds must be a whole number from 1 to 1024, using 64",
                PATH + ": world.max-chunk-sends-per-tick must be a whole number from 1 to 4096, using 16",
                PATH + ": world.encoder-threads must be a whole number from 1 to 16, using 2"), warnings);
    }

    private ConfigReader reader(Map<String, Object> server) throws IOException {
        return new ConfigReader(PATH, server::get, ShippedConfig.values()::get, warnings::add);
    }
}

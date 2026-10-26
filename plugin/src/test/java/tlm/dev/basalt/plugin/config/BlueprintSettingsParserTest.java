package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.blueprint.PlanSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlueprintSettingsParserTest {

    private static final String PATH = "plugins/BasaltCore/config.yml";

    private final List<String> warnings = new ArrayList<>();

    @Test
    void shipsAMillionBlocksAndThirtyTwoMegabytes() throws IOException {
        assertEquals("1000000", ShippedConfig.entries().get(BlueprintSettingsParser.MAX_BLOCKS));
        assertEquals("33554432", ShippedConfig.entries().get(BlueprintSettingsParser.MAX_FILE_BYTES));
        assertEquals(new PlanSettings(1_000_000, 33_554_432), parse(Map.of()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheServerValuesAndRefusesBadOnes() throws IOException {
        assertEquals(new PlanSettings(5000, 2048), parse(Map.of(BlueprintSettingsParser.MAX_BLOCKS, 5000, BlueprintSettingsParser.MAX_FILE_BYTES, 2048)));
        assertEquals(new PlanSettings(1_000_000, 33_554_432), parse(Map.of(BlueprintSettingsParser.MAX_BLOCKS, 0, BlueprintSettingsParser.MAX_FILE_BYTES, 10)));

        assertEquals(List.of(PATH + ": blueprint.max-blocks must be a whole number from 1 to 16777216, using 1000000",
                PATH + ": blueprint.max-file-bytes must be a whole number from 1024 to 268435456, using 33554432"), warnings);
    }

    private PlanSettings parse(Map<String, Object> server) throws IOException {
        return BlueprintSettingsParser.parse(new ConfigReader(PATH, server::get, ShippedConfig.values()::get, warnings::add));
    }
}

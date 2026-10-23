package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.visibility.EntityMaskSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityMaskSettingsParserTest {

    private static final String PATH = "plugins/BasaltCore/config.yml";

    private final List<String> warnings = new ArrayList<>();

    @Test
    void shipsTheDefaultScan() throws IOException {
        Map<String, String> entries = ShippedConfig.entries();
        assertEquals("5", entries.get(EntityMaskSettingsParser.SCAN_INTERVAL_TICKS));
        assertEquals("256", entries.get(EntityMaskSettingsParser.MAX_ENTITIES_PER_SCAN));
        assertEquals(new EntityMaskSettings(5, 256), parse(Map.of()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheServerValues() throws IOException {
        assertEquals(new EntityMaskSettings(20, 64), parse(Map.of(
                EntityMaskSettingsParser.SCAN_INTERVAL_TICKS, 20, EntityMaskSettingsParser.MAX_ENTITIES_PER_SCAN, 64)));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void keepsTheShippedValueForABadOneAndSaysWhere() throws IOException {
        assertEquals(new EntityMaskSettings(5, 256), parse(Map.of(
                EntityMaskSettingsParser.SCAN_INTERVAL_TICKS, 0, EntityMaskSettingsParser.MAX_ENTITIES_PER_SCAN, "many")));

        assertEquals(List.of(
                PATH + ": visibility.entity-scan-interval-ticks must be a whole number from 1 to 200, using 5",
                PATH + ": visibility.max-entities-per-scan must be a whole number from 1 to 10000, using 256"), warnings);
    }

    @Test
    void everyShippedKeyIsReadByAParser() throws IOException {
        assertEquals(List.of(InteractionSettingsParser.MAX_ACTIONS_PER_SECOND, InteractionSettingsParser.REACH_MARGIN,
                InteractionSettingsParser.ENTITY_REACH_MARGIN,
                EntityMaskSettingsParser.SCAN_INTERVAL_TICKS, EntityMaskSettingsParser.MAX_ENTITIES_PER_SCAN),
                List.copyOf(ShippedConfig.entries().keySet()));
    }

    private EntityMaskSettings parse(Map<String, Object> server) throws IOException {
        return EntityMaskSettingsParser.parse(new ConfigReader(PATH, server::get, ShippedConfig.values()::get, warnings::add));
    }
}

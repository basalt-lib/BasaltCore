package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.entity.EntitySettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntitySettingsParserTest {

    private static final String PATH = "plugins/BasaltCore/config.yml";

    private final List<String> warnings = new ArrayList<>();

    @Test
    void shipsSixteenEntitiesPerWorld() throws IOException {
        assertEquals("16", ShippedConfig.entries().get(EntitySettingsParser.MAX_PER_WORLD));
        assertEquals(new EntitySettings(16), parse(Map.of()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheServerValueAndRefusesABadOne() throws IOException {
        assertEquals(new EntitySettings(4), parse(Map.of(EntitySettingsParser.MAX_PER_WORLD, 4)));
        assertEquals(new EntitySettings(16), parse(Map.of(EntitySettingsParser.MAX_PER_WORLD, 5000)));

        assertEquals(List.of(PATH + ": entity.max-per-world must be a whole number from 1 to 1024, using 16"), warnings);
    }

    private EntitySettings parse(Map<String, Object> server) throws IOException {
        return EntitySettingsParser.parse(new ConfigReader(PATH, server::get, ShippedConfig.values()::get, warnings::add));
    }
}

package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.interaction.InteractionSettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionSettingsParserTest {

    private static final String PATH = "plugins/BasaltCore/config.yml";

    private final List<String> warnings = new ArrayList<>();

    @Test
    void shipsTheDefaultLimits() throws IOException {
        Map<String, String> entries = ShippedConfig.entries();
        assertEquals("20", entries.get(InteractionSettingsParser.MAX_ACTIONS_PER_SECOND));
        assertEquals("1.0", entries.get(InteractionSettingsParser.REACH_MARGIN));
        assertEquals("3.0", entries.get(InteractionSettingsParser.ENTITY_REACH_MARGIN));
        assertEquals(new InteractionSettings(20, 1.0, 3.0), parse(Map.of()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheServerValues() throws IOException {
        assertEquals(new InteractionSettings(40, 0.5, 3.0), parse(Map.of(
                InteractionSettingsParser.MAX_ACTIONS_PER_SECOND, 40, InteractionSettingsParser.REACH_MARGIN, 0.5)));
        assertEquals(new InteractionSettings(20, 2.0, 3.0), parse(Map.of(InteractionSettingsParser.REACH_MARGIN, 2)));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void keepsTheShippedValueForABadOneAndSaysWhere() throws IOException {
        assertEquals(new InteractionSettings(20, 1.0, 3.0), parse(Map.of(
                InteractionSettingsParser.MAX_ACTIONS_PER_SECOND, 0, InteractionSettingsParser.REACH_MARGIN, "lots")));
        assertEquals(new InteractionSettings(20, 1.0, 3.0), parse(Map.of(
                InteractionSettingsParser.MAX_ACTIONS_PER_SECOND, 2.5, InteractionSettingsParser.REACH_MARGIN, 17)));

        assertEquals(4, warnings.size());
        assertTrue(warnings.get(0).startsWith(PATH + ": interaction.max-actions-per-second must be a whole number from 1 to 1000, using 20"), warnings.get(0));
        assertTrue(warnings.get(1).startsWith(PATH + ": interaction.reach-margin must be a number from 0.0 to 16.0, using 1.0"), warnings.get(1));
    }

    @Test
    void refusesABrokenShippedFile() {
        assertThrows(IllegalStateException.class, () -> InteractionSettingsParser.parse(new ConfigReader(PATH, key -> null, key -> null, warnings::add)));
    }

    private InteractionSettings parse(Map<String, Object> server) throws IOException {
        return InteractionSettingsParser.parse(new ConfigReader(PATH, server::get, ShippedConfig.values()::get, warnings::add));
    }
}

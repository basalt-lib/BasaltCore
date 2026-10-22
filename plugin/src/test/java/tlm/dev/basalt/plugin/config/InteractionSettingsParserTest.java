package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.interaction.InteractionSettings;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionSettingsParserTest {

    private static final String PATH = "plugins/BasaltCore/config.yml";
    private static final Pattern ENTRY = Pattern.compile("(?m)^  ([a-z-]+): ([0-9.]+)$");

    private final List<String> warnings = new ArrayList<>();

    @Test
    void shipsTheDefaultLimits() throws IOException {
        assertEquals(Map.of("max-actions-per-second", "20", "reach-margin", "1.0"), shippedEntries());
        assertEquals(new InteractionSettings(20, 1.0), parse(Map.of()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheServerValues() throws IOException {
        assertEquals(new InteractionSettings(40, 0.5), parse(Map.of(
                InteractionSettingsParser.MAX_ACTIONS_PER_SECOND, 40, InteractionSettingsParser.REACH_MARGIN, 0.5)));
        assertEquals(new InteractionSettings(20, 2.0), parse(Map.of(InteractionSettingsParser.REACH_MARGIN, 2)));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void keepsTheShippedValueForABadOneAndSaysWhere() throws IOException {
        assertEquals(new InteractionSettings(20, 1.0), parse(Map.of(
                InteractionSettingsParser.MAX_ACTIONS_PER_SECOND, 0, InteractionSettingsParser.REACH_MARGIN, "lots")));
        assertEquals(new InteractionSettings(20, 1.0), parse(Map.of(
                InteractionSettingsParser.MAX_ACTIONS_PER_SECOND, 2.5, InteractionSettingsParser.REACH_MARGIN, 17)));

        assertEquals(4, warnings.size());
        assertTrue(warnings.get(0).startsWith(PATH + ": interaction.max-actions-per-second must be a whole number from 1 to 1000, using 20"), warnings.get(0));
        assertTrue(warnings.get(1).startsWith(PATH + ": interaction.reach-margin must be a number from 0 to 16.0, using 1.0"), warnings.get(1));
    }

    @Test
    void refusesABrokenShippedFile() {
        assertThrows(IllegalStateException.class, () -> InteractionSettingsParser.parse(PATH, key -> null, key -> null, warnings::add));
    }

    private InteractionSettings parse(Map<String, Object> server) throws IOException {
        Map<String, Object> shipped = new HashMap<>();
        shippedEntries().forEach((key, value) -> shipped.put("interaction." + key,
                value.contains(".") ? (Object) Double.valueOf(value) : (Object) Integer.valueOf(value)));
        return InteractionSettingsParser.parse(PATH, server::get, shipped::get, warnings::add);
    }

    private Map<String, String> shippedEntries() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/config.yml")) {
            assertNotNull(input, "config.yml is missing");
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
            assertTrue(text.startsWith("interaction:\n"), text);
            Matcher matcher = ENTRY.matcher(text);
            Map<String, String> entries = new HashMap<>();
            while (matcher.find()) {
                entries.put(matcher.group(1), matcher.group(2));
            }
            return entries;
        }
    }
}

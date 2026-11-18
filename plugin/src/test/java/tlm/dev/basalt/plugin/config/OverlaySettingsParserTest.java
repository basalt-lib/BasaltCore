package tlm.dev.basalt.plugin.config;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.overlay.OverlaySettings;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlaySettingsParserTest {

    private static final String PATH = "plugins/BasaltCore/config.yml";

    private final List<String> warnings = new ArrayList<>();

    @Test
    void shipsRoomForTwoHundredPlayersWithFiftyBlocksEach() throws IOException {
        Map<String, String> shipped = ShippedConfig.entries();
        assertEquals("1024", shipped.get(OverlaySettingsParser.MAX_OVERLAYS));
        assertEquals("4096", shipped.get(OverlaySettingsParser.MAX_BLOCKS));
        assertEquals("256", shipped.get(OverlaySettingsParser.MAX_ENTITIES));
        assertEquals(new OverlaySettings(1024, 4096, 256), parse(Map.of()));
        assertTrue(warnings.isEmpty());
    }

    @Test
    void readsTheServerValuesAndRefusesABadOne() throws IOException {
        assertEquals(new OverlaySettings(10, 20, 30), parse(Map.of(
                OverlaySettingsParser.MAX_OVERLAYS, 10, OverlaySettingsParser.MAX_BLOCKS, 20, OverlaySettingsParser.MAX_ENTITIES, 30)));
        assertEquals(new OverlaySettings(1024, 4096, 256), parse(Map.of(OverlaySettingsParser.MAX_ENTITIES, 0)));

        assertEquals(List.of(PATH + ": overlay.max-entities must be a whole number from 1 to 4096, using 256"), warnings);
    }

    private OverlaySettings parse(Map<String, Object> server) throws IOException {
        return OverlaySettingsParser.parse(new ConfigReader(PATH, server::get, ShippedConfig.values()::get, warnings::add));
    }
}

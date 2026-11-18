package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.overlay.OverlaySettings;

public final class OverlaySettingsParser {

    public static final String MAX_OVERLAYS = "overlay.max-overlays";
    public static final String MAX_BLOCKS = "overlay.max-blocks";
    public static final String MAX_ENTITIES = "overlay.max-entities";

    private OverlaySettingsParser() {
    }

    public static OverlaySettings parse(ConfigReader config) {
        return new OverlaySettings(
                config.wholeNumber(MAX_OVERLAYS, 1, OverlaySettings.MAX_OVERLAYS_LIMIT),
                config.wholeNumber(MAX_BLOCKS, 1, OverlaySettings.MAX_BLOCKS_LIMIT),
                config.wholeNumber(MAX_ENTITIES, 1, OverlaySettings.MAX_ENTITIES_LIMIT));
    }
}

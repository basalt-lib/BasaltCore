package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.visibility.EntityMaskSettings;

public final class EntityMaskSettingsParser {

    public static final String SCAN_INTERVAL_TICKS = "visibility.entity-scan-interval-ticks";
    public static final String MAX_ENTITIES_PER_SCAN = "visibility.max-entities-per-scan";

    private EntityMaskSettingsParser() {
    }

    public static EntityMaskSettings parse(ConfigReader config) {
        return new EntityMaskSettings(
                config.wholeNumber(SCAN_INTERVAL_TICKS, 1, EntityMaskSettings.MAX_SCAN_INTERVAL),
                config.wholeNumber(MAX_ENTITIES_PER_SCAN, 1, EntityMaskSettings.MAX_ENTITIES_LIMIT));
    }
}

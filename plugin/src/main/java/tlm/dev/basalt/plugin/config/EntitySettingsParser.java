package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.entity.EntitySettings;

public final class EntitySettingsParser {

    public static final String MAX_PER_WORLD = "entity.max-per-world";

    private EntitySettingsParser() {
    }

    public static EntitySettings parse(ConfigReader config) {
        return new EntitySettings(config.wholeNumber(MAX_PER_WORLD, 1, EntitySettings.MAX_PER_WORLD_LIMIT));
    }
}

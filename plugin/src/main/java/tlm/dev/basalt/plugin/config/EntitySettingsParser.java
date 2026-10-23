package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.entity.EntitySettings;

public final class EntitySettingsParser {

    public static final String MAX_PER_PLAYER = "entity.max-per-player";

    private EntitySettingsParser() {
    }

    public static EntitySettings parse(ConfigReader config) {
        return new EntitySettings(config.wholeNumber(MAX_PER_PLAYER, 1, EntitySettings.MAX_PER_PLAYER_LIMIT));
    }
}

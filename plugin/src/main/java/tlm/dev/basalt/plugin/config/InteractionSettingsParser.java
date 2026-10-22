package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.interaction.InteractionSettings;

public final class InteractionSettingsParser {

    public static final String FILE_NAME = "config.yml";
    public static final String MAX_ACTIONS_PER_SECOND = "interaction.max-actions-per-second";
    public static final String REACH_MARGIN = "interaction.reach-margin";

    private InteractionSettingsParser() {
    }

    public static InteractionSettings parse(ConfigReader config) {
        return new InteractionSettings(
                config.wholeNumber(MAX_ACTIONS_PER_SECOND, 1, InteractionSettings.MAX_ACTIONS_LIMIT),
                config.number(REACH_MARGIN, 0, InteractionSettings.MAX_REACH_MARGIN));
    }
}

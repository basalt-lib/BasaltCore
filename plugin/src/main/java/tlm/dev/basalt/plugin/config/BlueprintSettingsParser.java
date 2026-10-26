package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.blueprint.PlanSettings;

public final class BlueprintSettingsParser {

    public static final String MAX_BLOCKS = "blueprint.max-blocks";
    public static final String MAX_FILE_BYTES = "blueprint.max-file-bytes";

    private BlueprintSettingsParser() {
    }

    public static PlanSettings parse(ConfigReader config) {
        return new PlanSettings(
                config.wholeNumber(MAX_BLOCKS, 1, PlanSettings.MAX_BLOCKS_LIMIT),
                config.wholeNumber(MAX_FILE_BYTES, PlanSettings.MIN_FILE_BYTES, PlanSettings.MAX_FILE_BYTES_LIMIT));
    }
}

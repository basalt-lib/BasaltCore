package tlm.dev.basalt.plugin.config;

import tlm.dev.basalt.core.interaction.InteractionSettings;

import java.util.function.Consumer;
import java.util.function.Function;

public final class InteractionSettingsParser {

    public static final String FILE_NAME = "config.yml";
    public static final String MAX_ACTIONS_PER_SECOND = "interaction.max-actions-per-second";
    public static final String REACH_MARGIN = "interaction.reach-margin";

    private InteractionSettingsParser() {
    }

    public static InteractionSettings parse(String path, Function<String, Object> server, Function<String, Object> shipped, Consumer<String> warnings) {
        if (!(shipped.apply(MAX_ACTIONS_PER_SECOND) instanceof Integer shippedActions)
                || !(shipped.apply(REACH_MARGIN) instanceof Number shippedMargin)) {
            throw new IllegalStateException("the shipped " + FILE_NAME + " has no valid " + MAX_ACTIONS_PER_SECOND + " or " + REACH_MARGIN);
        }
        InteractionSettings defaults = new InteractionSettings(shippedActions, shippedMargin.doubleValue());
        int actions = defaults.maxActionsPerSecond();
        Object actionsValue = server.apply(MAX_ACTIONS_PER_SECOND);
        if (actionsValue instanceof Integer value && value >= 1 && value <= InteractionSettings.MAX_ACTIONS_LIMIT) {
            actions = value;
        } else if (actionsValue != null) {
            warnings.accept(path + ": " + MAX_ACTIONS_PER_SECOND + " must be a whole number from 1 to "
                    + InteractionSettings.MAX_ACTIONS_LIMIT + ", using " + defaults.maxActionsPerSecond());
        }
        double margin = defaults.reachMargin();
        Object marginValue = server.apply(REACH_MARGIN);
        if (marginValue instanceof Number value && Double.isFinite(value.doubleValue())
                && value.doubleValue() >= 0 && value.doubleValue() <= InteractionSettings.MAX_REACH_MARGIN) {
            margin = value.doubleValue();
        } else if (marginValue != null) {
            warnings.accept(path + ": " + REACH_MARGIN + " must be a number from 0 to "
                    + InteractionSettings.MAX_REACH_MARGIN + ", using " + defaults.reachMargin());
        }
        return new InteractionSettings(actions, margin);
    }
}

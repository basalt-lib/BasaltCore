package tlm.dev.basalt.core.interaction;

public record InteractionSettings(int maxActionsPerSecond, double reachMargin, double entityReachMargin) {

    public static final int MAX_ACTIONS_LIMIT = 1000;
    public static final double MAX_REACH_MARGIN = 16.0;

    public InteractionSettings {
        if (maxActionsPerSecond < 1 || maxActionsPerSecond > MAX_ACTIONS_LIMIT) {
            throw new IllegalArgumentException("max actions per second must be from 1 to " + MAX_ACTIONS_LIMIT + ": " + maxActionsPerSecond);
        }
        if (!Double.isFinite(reachMargin) || reachMargin < 0 || reachMargin > MAX_REACH_MARGIN) {
            throw new IllegalArgumentException("reach margin must be from 0 to " + MAX_REACH_MARGIN + ": " + reachMargin);
        }
        if (!Double.isFinite(entityReachMargin) || entityReachMargin < 0 || entityReachMargin > MAX_REACH_MARGIN) {
            throw new IllegalArgumentException("entity reach margin must be from 0 to " + MAX_REACH_MARGIN + ": " + entityReachMargin);
        }
    }
}

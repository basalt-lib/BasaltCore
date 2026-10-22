package tlm.dev.basalt.core.interaction;

public final class ActionBudget {

    private static final double TICKS_PER_SECOND = 20.0;

    private final double capacity;
    private final double refillPerTick;
    private double tokens;
    private long lastTick;
    private boolean used;

    public ActionBudget(int actionsPerSecond) {
        if (actionsPerSecond < 1) {
            throw new IllegalArgumentException("a budget allows at least one action per second");
        }
        this.capacity = actionsPerSecond;
        this.refillPerTick = actionsPerSecond / TICKS_PER_SECOND;
        this.tokens = capacity;
    }

    public boolean take(long tick) {
        if (used) {
            tokens = Math.min(capacity, tokens + Math.max(0, tick - lastTick) * refillPerTick);
        }
        used = true;
        lastTick = Math.max(lastTick, tick);
        if (tokens < 1) {
            return false;
        }
        tokens -= 1;
        return true;
    }
}

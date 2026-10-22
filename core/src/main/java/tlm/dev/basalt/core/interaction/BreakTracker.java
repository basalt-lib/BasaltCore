package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.BlockPosition;

import java.util.Optional;

public final class BreakTracker {

    private BlockPosition breaking;
    private long breakingSince;
    private BlockPosition delayed;
    private long delayedSince;

    public Optional<BlockPosition> start(BlockPosition position, long tick) {
        BlockPosition previous = breaking;
        breaking = position;
        breakingSince = tick;
        return previous == null || previous.equals(position) ? Optional.empty() : Optional.of(previous);
    }

    public BreakStop stop(BlockPosition position, long tick, float progressPerTick) {
        if (breaking == null || !breaking.equals(position)) {
            return BreakStop.NOT_BREAKING;
        }
        if (InteractionValidator.breaksAtStop(progressPerTick, tick - breakingSince)) {
            breaking = null;
            return BreakStop.BROKEN;
        }
        if (delayed == null) {
            delayed = breaking;
            delayedSince = breakingSince;
            breaking = null;
        }
        return BreakStop.DELAYED;
    }

    public void abort() {
        breaking = null;
    }

    public Optional<BlockPosition> delayed() {
        return Optional.ofNullable(delayed);
    }

    public long delayedSince() {
        return delayedSince;
    }

    public void clearDelayed() {
        delayed = null;
    }
}

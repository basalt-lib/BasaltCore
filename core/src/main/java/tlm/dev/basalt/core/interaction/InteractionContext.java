package tlm.dev.basalt.core.interaction;

import tlm.dev.basalt.core.position.Point;

import java.util.OptionalInt;

public record InteractionContext(long tick, GameMode mode, Point eyes, double reach, OptionalInt heldBlock) {

    public InteractionContext {
        if (mode == null || eyes == null || heldBlock == null) {
            throw new IllegalArgumentException("a context needs a game mode, the eyes and the held block or none");
        }
        if (!Double.isFinite(reach) || reach < 0) {
            throw new IllegalArgumentException("reach must be a distance of 0 or more: " + reach);
        }
    }
}

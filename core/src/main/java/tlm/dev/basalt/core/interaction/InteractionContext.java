package tlm.dev.basalt.core.interaction;

import java.util.OptionalInt;

public record InteractionContext(long tick, GameMode mode, PlayerStance stance, OptionalInt heldBlock) {

    public InteractionContext {
        if (mode == null || stance == null || heldBlock == null) {
            throw new IllegalArgumentException("a context needs a game mode, the stance and the held block or none");
        }
    }
}

package tlm.dev.basalt.core.interaction;

public record InteractionContext(long tick, GameMode mode, PlayerStance stance, HeldItem held) {

    public InteractionContext {
        if (mode == null || stance == null || held == null) {
            throw new IllegalArgumentException("a context needs a game mode, the stance and the held item");
        }
    }
}

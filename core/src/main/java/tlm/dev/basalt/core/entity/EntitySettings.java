package tlm.dev.basalt.core.entity;

public record EntitySettings(int maxPerPlayer) {

    public static final int MAX_PER_PLAYER_LIMIT = 1024;

    public EntitySettings {
        if (maxPerPlayer < 1 || maxPerPlayer > MAX_PER_PLAYER_LIMIT) {
            throw new IllegalArgumentException("entities per player must be from 1 to " + MAX_PER_PLAYER_LIMIT + ": " + maxPerPlayer);
        }
    }
}

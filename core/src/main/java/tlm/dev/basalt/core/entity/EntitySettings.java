package tlm.dev.basalt.core.entity;

public record EntitySettings(int maxPerWorld) {

    public static final int MAX_PER_WORLD_LIMIT = 1024;

    public EntitySettings {
        if (maxPerWorld < 1 || maxPerWorld > MAX_PER_WORLD_LIMIT) {
            throw new IllegalArgumentException("entities per world must be from 1 to " + MAX_PER_WORLD_LIMIT + ": " + maxPerWorld);
        }
    }
}

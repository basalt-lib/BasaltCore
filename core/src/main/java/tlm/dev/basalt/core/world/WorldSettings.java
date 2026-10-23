package tlm.dev.basalt.core.world;

public record WorldSettings(int maxChunks, int maxWorlds) {

    public static final int MAX_CHUNKS_LIMIT = 4096;
    public static final int MAX_WORLDS_LIMIT = 1024;

    public WorldSettings {
        if (maxChunks < 1 || maxChunks > MAX_CHUNKS_LIMIT) {
            throw new IllegalArgumentException("chunks per world must be from 1 to " + MAX_CHUNKS_LIMIT + ": " + maxChunks);
        }
        if (maxWorlds < 1 || maxWorlds > MAX_WORLDS_LIMIT) {
            throw new IllegalArgumentException("open worlds must be from 1 to " + MAX_WORLDS_LIMIT + ": " + maxWorlds);
        }
    }
}

package tlm.dev.basalt.core.position;

public record ChunkKey(int x, int z) {

    public static final int MAX_COORDINATE = 1_875_000;

    public ChunkKey {
        if (x < -MAX_COORDINATE || x > MAX_COORDINATE || z < -MAX_COORDINATE || z > MAX_COORDINATE) {
            throw new IllegalArgumentException("chunk outside the world: " + x + ", " + z);
        }
    }

    public static ChunkKey ofBlock(int blockX, int blockZ) {
        return new ChunkKey(blockX >> 4, blockZ >> 4);
    }

    public long packed() {
        return (x & 0xFFFFFFFFL) | (z & 0xFFFFFFFFL) << 32;
    }
}

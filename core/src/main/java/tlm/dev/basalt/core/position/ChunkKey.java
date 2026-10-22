package tlm.dev.basalt.core.position;

import java.util.Optional;

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

    public static Optional<ChunkKey> containing(double x, double z) {
        if (!Double.isFinite(x) || !Double.isFinite(z)) {
            return Optional.empty();
        }
        double chunkX = Math.floor(x / 16);
        double chunkZ = Math.floor(z / 16);
        if (Math.abs(chunkX) > MAX_COORDINATE || Math.abs(chunkZ) > MAX_COORDINATE) {
            return Optional.empty();
        }
        return Optional.of(new ChunkKey((int) chunkX, (int) chunkZ));
    }

    public long packed() {
        return (x & 0xFFFFFFFFL) | (z & 0xFFFFFFFFL) << 32;
    }
}

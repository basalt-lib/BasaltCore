package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;

public final class MovementPolicy {

    public static final double PLAYER_HALF_WIDTH = 0.3;

    private MovementPolicy() {
    }

    public static boolean relaxes(Optional<ChunkKey> virtualChunk, double x, double z) {
        return virtualChunk.filter(chunk -> touches(chunk, x, z)).isPresent();
    }

    private static boolean touches(ChunkKey chunk, double x, double z) {
        return chunk.x() >= chunkOf(x - PLAYER_HALF_WIDTH) && chunk.x() <= chunkOf(x + PLAYER_HALF_WIDTH)
                && chunk.z() >= chunkOf(z - PLAYER_HALF_WIDTH) && chunk.z() <= chunkOf(z + PLAYER_HALF_WIDTH);
    }

    private static int chunkOf(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }
}

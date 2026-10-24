package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkRange;

import java.util.Optional;

public final class MovementPolicy {

    public static final double PLAYER_HALF_WIDTH = 0.3;

    private MovementPolicy() {
    }

    public static boolean relaxes(Optional<ChunkRange> area, double x, double z) {
        return area.filter(range -> range.overlaps(chunkOf(x - PLAYER_HALF_WIDTH), chunkOf(z - PLAYER_HALF_WIDTH),
                chunkOf(x + PLAYER_HALF_WIDTH), chunkOf(z + PLAYER_HALF_WIDTH))).isPresent();
    }

    private static int chunkOf(double coordinate) {
        return (int) Math.floor(coordinate) >> 4;
    }
}

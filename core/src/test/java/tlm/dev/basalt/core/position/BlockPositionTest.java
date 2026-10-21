package tlm.dev.basalt.core.position;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockPositionTest {

    @Test
    void findsItsChunkAndLocalCoordinatesBelowZero() {
        BlockPosition block = new BlockPosition(-1, -64, -17);

        assertEquals(new ChunkKey(-1, -2), block.chunk());
        assertEquals(15, block.localX());
        assertEquals(15, block.localZ());
        assertEquals(new ChunkKey(64, 64), new BlockPosition(1039, -58, 1024).chunk());
    }

    @Test
    void stepsThroughEachFace() {
        BlockPosition block = new BlockPosition(1029, -58, 1029);

        assertEquals(new BlockPosition(1029, -57, 1029), block.relative(BlockFace.UP));
        assertEquals(new BlockPosition(1029, -59, 1029), block.relative(BlockFace.DOWN));
        assertEquals(new BlockPosition(1029, -58, 1028), block.relative(BlockFace.NORTH));
        assertEquals(new BlockPosition(1029, -58, 1030), block.relative(BlockFace.SOUTH));
        assertEquals(new BlockPosition(1028, -58, 1029), block.relative(BlockFace.WEST));
        assertEquals(new BlockPosition(1030, -58, 1029), block.relative(BlockFace.EAST));
    }

    @Test
    void staysInsideTheWorld() {
        assertThrows(IllegalArgumentException.class, () -> new BlockPosition(30_000_001, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new BlockPosition(0, 4097, 0));
        assertThrows(IllegalArgumentException.class, () -> new BlockPosition(0, 0, -30_000_001));
    }

    @Test
    void pointsKnowWhenTheyAreFinite() {
        assertEquals(new Point(1029.5, -57.5, 1029.5), Point.centerOf(new BlockPosition(1029, -58, 1029)));
        assertTrue(new Point(0, 0, 0).isFinite());
        assertFalse(new Point(Double.NaN, 0, 0).isFinite());
        assertFalse(new Point(0, Double.POSITIVE_INFINITY, 0).isFinite());
    }
}

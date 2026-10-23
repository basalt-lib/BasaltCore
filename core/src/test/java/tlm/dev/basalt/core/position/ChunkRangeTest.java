package tlm.dev.basalt.core.position;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkRangeTest {

    @Test
    void holdsTheChunksBetweenItsCornersIncluded() {
        ChunkRange range = new ChunkRange(-1, 2, 0, 3);

        assertTrue(range.contains(new ChunkKey(-1, 2)));
        assertTrue(range.contains(new ChunkKey(0, 3)));
        assertFalse(range.contains(new ChunkKey(1, 3)));
        assertFalse(range.contains(new ChunkKey(0, 1)));
        assertEquals(4, range.chunkCount());
    }

    @Test
    void ordersTheCornersItIsGiven() {
        assertEquals(new ChunkRange(-3, 1, 5, 4), ChunkRange.between(new ChunkKey(5, 1), new ChunkKey(-3, 4)));
        assertEquals(new ChunkRange(7, 7, 7, 7), ChunkRange.of(new ChunkKey(7, 7)));
    }

    @Test
    void refusesAnUpsideDownRangeOrOneOutsideTheWorld() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkRange(1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkRange(0, 0, ChunkKey.MAX_COORDINATE + 1, 0));
    }

    @Test
    void findsTheChunkOfABlockColumn() {
        ChunkRange range = new ChunkRange(64, 64, 65, 64);

        assertTrue(range.containsBlock(1024, 1039));
        assertTrue(range.containsBlock(1055, 1024));
        assertFalse(range.containsBlock(1056, 1024));
        assertFalse(range.containsBlock(1023, 1030));
        assertFalse(new ChunkRange(-1, -1, -1, -1).containsBlock(0, -1));
        assertTrue(new ChunkRange(-1, -1, -1, -1).containsBlock(-16, -1));
    }

    @Test
    void listsItsChunksRowByRow() {
        assertEquals(List.of(new ChunkKey(0, 0), new ChunkKey(1, 0), new ChunkKey(0, 1), new ChunkKey(1, 1)),
                new ChunkRange(0, 0, 1, 1).chunks());
    }

    @Test
    void tellsWhetherAnotherRectangleOverlapsIt() {
        ChunkRange range = new ChunkRange(0, 0, 3, 3);

        assertTrue(range.overlaps(3, 3, 9, 9));
        assertTrue(range.overlaps(-5, 1, 0, 1));
        assertFalse(range.overlaps(4, 0, 9, 3));
        assertFalse(range.overlaps(0, -2, 3, -1));
    }
}

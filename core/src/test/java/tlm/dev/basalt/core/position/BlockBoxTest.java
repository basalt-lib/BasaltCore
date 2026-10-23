package tlm.dev.basalt.core.position;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockBoxTest {

    @Test
    void ordersTheCornersItIsGiven() {
        BlockBox box = BlockBox.between(new BlockPosition(5, -60, -3), new BlockPosition(-2, -64, 4));

        assertEquals(new BlockPosition(-2, -64, -3), box.min());
        assertEquals(new BlockPosition(5, -60, 4), box.max());
        assertEquals(8L * 5 * 8, box.volume());
    }

    @Test
    void refusesAnUpsideDownBox() {
        assertThrows(IllegalArgumentException.class, () -> new BlockBox(new BlockPosition(1, 0, 0), new BlockPosition(0, 0, 0)));
    }

    @Test
    void holdsTheBlocksBetweenItsCornersIncluded() {
        BlockBox box = BlockBox.between(new BlockPosition(0, 0, 0), new BlockPosition(2, 2, 2));

        assertTrue(box.contains(new BlockPosition(2, 0, 1)));
        assertFalse(box.contains(new BlockPosition(3, 0, 1)));
        assertTrue(BlockBox.of(new BlockPosition(7, 7, 7)).contains(new BlockPosition(7, 7, 7)));
    }

    @Test
    void spansTheChunksUnderIt() {
        BlockBox box = BlockBox.between(new BlockPosition(-1, 0, 15), new BlockPosition(16, 0, 32));

        assertEquals(new ChunkRange(-1, 0, 1, 2), box.chunks());
    }

    @Test
    void cutsThePartInsideAChunkColumn() {
        BlockBox box = BlockBox.between(new BlockPosition(10, -64, -5), new BlockPosition(40, -60, 3));

        assertEquals(Optional.of(BlockBox.between(new BlockPosition(16, -64, 0), new BlockPosition(31, -60, 3))),
                box.within(new ChunkKey(1, 0)));
        assertEquals(Optional.of(BlockBox.between(new BlockPosition(10, -64, -5), new BlockPosition(15, -60, -1))),
                box.within(new ChunkKey(0, -1)));
        assertEquals(Optional.empty(), box.within(new ChunkKey(3, 0)));
    }
}

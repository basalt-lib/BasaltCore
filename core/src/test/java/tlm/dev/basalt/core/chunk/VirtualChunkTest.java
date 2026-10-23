package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualChunkTest {

    private static final TestBlocks TRAITS = new TestBlocks();

    private final VirtualChunk platform = new VirtualChunkBuilder(new ChunkKey(64, 64), TestBlocks.OVERWORLD, TestBlocks.AIR)
            .set(3, -54, 4, TestBlocks.STONE)
            .set(3, -53, 4, TestBlocks.FLOWER)
            .build(TestBlocks.FORMATS, TRAITS, TestBlocks.PLAINS);

    @Test
    void breakingABlockLeavesTheOriginalUntouched() {
        VirtualChunk broken = platform.withBlock(3, -54, 4, TestBlocks.AIR, TRAITS);

        assertEquals(TestBlocks.AIR, broken.stateAt(3, -54, 4));
        assertEquals(TestBlocks.STONE, platform.stateAt(3, -54, 4));
        assertEquals(1, broken.nonAirCount());
        assertEquals(2, platform.nonAirCount());
    }

    @Test
    void sharesEverySectionItDidNotChange() {
        VirtualChunk placed = platform.withBlock(0, 100, 0, TestBlocks.STONE, TRAITS);

        int changed = TestBlocks.OVERWORLD.sectionIndex(100);
        for (int index = 0; index < TestBlocks.OVERWORLD.sectionCount(); index++) {
            if (index == changed) {
                assertNotSame(platform.sections().get(index), placed.sections().get(index));
            } else {
                assertSame(platform.sections().get(index), placed.sections().get(index));
            }
        }
        assertEquals(1, placed.sections().get(changed).nonAirCount());
    }

    @Test
    void keepsTheHeightmapsRightForTheChangedColumn() {
        int column = HeightmapCodec.column(3, 4);
        assertEquals(12, platform.heights().get(HeightmapType.WORLD_SURFACE)[column]);
        assertEquals(11, platform.heights().get(HeightmapType.MOTION_BLOCKING)[column]);

        VirtualChunk broken = platform.withBlock(3, -54, 4, TestBlocks.AIR, TRAITS);
        assertEquals(12, broken.heights().get(HeightmapType.WORLD_SURFACE)[column]);
        assertEquals(0, broken.heights().get(HeightmapType.MOTION_BLOCKING)[column]);

        VirtualChunk raised = broken.withBlock(3, 300, 4, TestBlocks.LEAVES, TRAITS);
        assertEquals(365, raised.heights().get(HeightmapType.MOTION_BLOCKING)[column]);
        assertEquals(0, raised.heights().get(HeightmapType.MOTION_BLOCKING_NO_LEAVES)[column]);
        assertEquals(0, raised.heights().get(HeightmapType.WORLD_SURFACE)[HeightmapCodec.column(4, 4)]);
    }

    @Test
    void returnsItselfWhenTheBlockDoesNotChange() {
        assertSame(platform, platform.withBlock(3, -54, 4, TestBlocks.STONE, TRAITS));
    }

    @Test
    void fillsABoxAcrossSectionsInOneCopy() {
        VirtualChunk filled = platform.filled(new LocalBox(0, -50, 0, 15, -47, 1), TestBlocks.STONE, TRAITS);

        assertEquals(2 + 16 * 4 * 2, filled.nonAirCount());
        assertEquals(16 * 2 * 2 + 2, filled.sections().get(0).nonAirCount());
        assertEquals(16 * 2 * 2, filled.sections().get(1).nonAirCount());
        assertEquals(TestBlocks.STONE, filled.stateAt(15, -47, 1));
        assertEquals(TestBlocks.AIR, filled.stateAt(15, -47, 2));
        assertSame(platform.sections().get(2), filled.sections().get(2));
        assertEquals(18, filled.heights().get(HeightmapType.MOTION_BLOCKING)[HeightmapCodec.column(7, 1)]);
        assertEquals(0, filled.heights().get(HeightmapType.MOTION_BLOCKING)[HeightmapCodec.column(7, 2)]);
    }

    @Test
    void fillingWithAirEmptiesTheBox() {
        VirtualChunk cleared = platform.filled(new LocalBox(0, -64, 0, 15, 319, 15), TestBlocks.AIR, TRAITS);

        assertEquals(0, cleared.nonAirCount());
        assertEquals(0, cleared.heights().get(HeightmapType.WORLD_SURFACE)[HeightmapCodec.column(3, 4)]);
    }

    @Test
    void fillReturnsItselfWhenEveryBlockAlreadyMatches() {
        assertSame(platform, platform.filled(new LocalBox(5, 100, 5, 9, 120, 9), TestBlocks.AIR, TRAITS));
    }

    @Test
    void refusesALocalBoxOutsideTheChunkColumns() {
        assertThrows(IndexOutOfBoundsException.class, () -> new LocalBox(0, 0, 0, 16, 0, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> new LocalBox(-1, 0, 0, 3, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new LocalBox(3, 0, 0, 2, 0, 0));
        assertThrows(IndexOutOfBoundsException.class,
                () -> platform.filled(new LocalBox(0, 300, 0, 0, 320, 0), TestBlocks.STONE, TRAITS));
    }

    @Test
    void holdsOnlyItsOwnColumnsAndHeight() {
        assertTrue(platform.holds(0, -64, 0));
        assertTrue(platform.holds(15, 319, 15));
        assertFalse(platform.holds(16, 0, 0));
        assertFalse(platform.holds(0, -65, 0));
        assertFalse(platform.holds(0, 320, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> platform.withBlock(0, 320, 0, TestBlocks.STONE, TRAITS));
        assertThrows(IllegalArgumentException.class, () -> platform.withBlock(0, 0, 0, -1, TRAITS));
    }
}

package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VirtualChunkBuilderTest {

    private static final TestBlocks TRAITS = new TestBlocks();

    private final VirtualChunkBuilder builder = new VirtualChunkBuilder(new ChunkKey(3, -2), TestBlocks.OVERWORLD, TestBlocks.AIR);

    @Test
    void keepsTheBlocksItWasGiven() {
        builder.set(0, -64, 0, TestBlocks.STONE).set(15, 319, 15, TestBlocks.LEAVES);

        VirtualChunk chunk = build();

        assertEquals(new ChunkKey(3, -2), chunk.key());
        assertEquals(TestBlocks.STONE, chunk.stateAt(0, -64, 0));
        assertEquals(TestBlocks.LEAVES, chunk.stateAt(15, 319, 15));
        assertEquals(TestBlocks.AIR, chunk.stateAt(7, 100, 7));
    }

    @Test
    void countsNonAirBlocksPerSection() {
        builder.set(1, -64, 1, TestBlocks.STONE).set(2, -50, 2, TestBlocks.FLOWER).set(3, 0, 3, TestBlocks.STONE);

        VirtualChunk chunk = build();

        assertEquals(2, chunk.sections().get(0).nonAirCount());
        assertEquals(1, chunk.sections().get(4).nonAirCount());
        assertEquals(0, chunk.sections().get(10).nonAirCount());
        assertEquals(3, chunk.nonAirCount());
    }

    @Test
    void leavesUntouchedSectionsAsSingleAirValues() {
        VirtualChunk chunk = build();

        for (ChunkSection section : chunk.sections()) {
            assertEquals(0, section.states().bits());
            assertEquals(TestBlocks.AIR, section.states().get(0));
        }
    }

    @Test
    void ignoresAirPlacedInAnEmptySection() {
        builder.set(0, 200, 0, TestBlocks.AIR);

        assertEquals(0, build().sections().get(TestBlocks.OVERWORLD.sectionIndex(200)).states().bits());
    }

    @Test
    void fillsEveryBiomeCellWithTheGivenBiome() {
        VirtualChunk chunk = build();

        assertEquals(TestBlocks.PLAINS, chunk.sections().get(0).biomes().get(0));
        assertEquals(TestBlocks.PLAINS, chunk.sections().get(23).biomes().get(63));
    }

    @Test
    void computesHeightmapsLikeTheServer() {
        builder.set(0, -54, 0, TestBlocks.STONE).set(0, -44, 0, TestBlocks.LEAVES).set(0, -40, 0, TestBlocks.FLOWER);

        VirtualChunk chunk = build();
        int column = HeightmapCodec.column(0, 0);

        assertEquals(25, chunk.heights().get(HeightmapType.WORLD_SURFACE)[column]);
        assertEquals(21, chunk.heights().get(HeightmapType.MOTION_BLOCKING)[column]);
        assertEquals(11, chunk.heights().get(HeightmapType.MOTION_BLOCKING_NO_LEAVES)[column]);
        assertEquals(0, chunk.heights().get(HeightmapType.WORLD_SURFACE)[HeightmapCodec.column(1, 0)]);
    }

    @Test
    void countsTheTopBlockOfTheWorld() {
        builder.set(4, 319, 4, TestBlocks.STONE);

        assertEquals(384, build().heights().get(HeightmapType.WORLD_SURFACE)[HeightmapCodec.column(4, 4)]);
    }

    @Test
    void returnsCopiesOfItsHeightmaps() {
        VirtualChunk chunk = build();

        assertNotSame(chunk.heights().get(HeightmapType.WORLD_SURFACE), chunk.heights().get(HeightmapType.WORLD_SURFACE));
    }

    @Test
    void rejectsBlocksOutsideTheChunk() {
        assertThrows(IndexOutOfBoundsException.class, () -> builder.set(16, 0, 0, TestBlocks.STONE));
        assertThrows(IndexOutOfBoundsException.class, () -> builder.set(0, 0, -1, TestBlocks.STONE));
        assertThrows(IndexOutOfBoundsException.class, () -> builder.set(0, -65, 0, TestBlocks.STONE));
        assertThrows(IndexOutOfBoundsException.class, () -> builder.set(0, 320, 0, TestBlocks.STONE));
    }

    @Test
    void rejectsNegativeStates() {
        assertThrows(IllegalArgumentException.class, () -> builder.set(0, 0, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> new VirtualChunkBuilder(new ChunkKey(0, 0), TestBlocks.OVERWORLD, -1));
    }

    private VirtualChunk build() {
        return builder.build(TestBlocks.FORMATS, TRAITS, TestBlocks.PLAINS);
    }
}

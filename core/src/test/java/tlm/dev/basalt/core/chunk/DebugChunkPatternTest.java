package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebugChunkPatternTest {

    private static final Map<String, Integer> STATES = Map.of(
            DebugChunkPattern.BEDROCK, 85,
            DebugChunkPattern.BASALT, 6200,
            DebugChunkPattern.SMOOTH_BASALT, 27000,
            DebugChunkPattern.GOLD_BLOCK, 2104,
            DebugChunkPattern.DIAMOND_BLOCK, 4775);

    @Test
    void putsTheSpecBlockCountInTheBottomSectionOnly() {
        VirtualChunk chunk = paint(TestBlocks.OVERWORLD);

        assertEquals(1087, chunk.sections().get(0).nonAirCount());
        assertEquals(1087, chunk.nonAirCount());
    }

    @Test
    void followsTheLevelsOfTheFlatTestWorld() {
        VirtualChunk chunk = paint(TestBlocks.OVERWORLD);

        assertEquals(85, chunk.stateAt(5, -64, 5));
        assertEquals(6200, chunk.stateAt(5, -63, 5));
        assertEquals(6200, chunk.stateAt(5, -62, 5));
        assertEquals(27000, chunk.stateAt(5, -61, 5));
        assertEquals(TestBlocks.AIR, chunk.stateAt(5, -60, 5));
    }

    @Test
    void ringsTheChunkWithSixtyGoldBlocks() {
        VirtualChunk chunk = paint(TestBlocks.OVERWORLD);
        int gold = 0;
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                if (chunk.stateAt(x, -60, z) == 2104) {
                    gold++;
                }
            }
        }

        assertEquals(60, gold);
        assertEquals(2104, chunk.stateAt(0, -60, 7));
        assertEquals(2104, chunk.stateAt(15, -60, 15));
    }

    @Test
    void raisesADiamondPillarInTheMiddle() {
        VirtualChunk chunk = paint(TestBlocks.OVERWORLD);

        assertEquals(4775, chunk.stateAt(8, -60, 8));
        assertEquals(4775, chunk.stateAt(8, -59, 8));
        assertEquals(4775, chunk.stateAt(8, -58, 8));
        assertEquals(TestBlocks.AIR, chunk.stateAt(8, -57, 8));
    }

    @Test
    void givesHeightmapsAboveTheTopBlocks() {
        Map<HeightmapType, int[]> heights = paint(TestBlocks.OVERWORLD).heights();

        assertEquals(7, heights.get(HeightmapType.MOTION_BLOCKING)[HeightmapCodec.column(8, 8)]);
        assertEquals(5, heights.get(HeightmapType.MOTION_BLOCKING)[HeightmapCodec.column(0, 0)]);
        assertEquals(4, heights.get(HeightmapType.WORLD_SURFACE)[HeightmapCodec.column(5, 5)]);
    }

    @Test
    void startsAtTheBottomOfAnyWorld() {
        VirtualChunk chunk = paint(new ChunkShape(0, 16));

        assertEquals(85, chunk.stateAt(5, 0, 5));
        assertEquals(4775, chunk.stateAt(8, 6, 8));
        assertEquals(1087, chunk.nonAirCount());
    }

    private static VirtualChunk paint(ChunkShape shape) {
        VirtualChunkBuilder builder = new VirtualChunkBuilder(new ChunkKey(0, 0), shape, TestBlocks.AIR);
        DebugChunkPattern.paint(builder, STATES::get);
        return builder.build(TestBlocks.FORMATS, new TestBlocks(), TestBlocks.PLAINS);
    }
}

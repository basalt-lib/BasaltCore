package tlm.dev.basalt.core.blueprint;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.edit.ChunkWrite;
import tlm.dev.basalt.core.edit.SectionWrite;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockPlacerTest {

    private static final int MIN_Y = -64;
    private static final int SECTIONS = 24;

    private final BlockPlan plan = sample(new PlanSize(37, 20, 19));

    @Test
    void theAlignedPathWritesExactlyWhatTheGeneralPathWrites() {
        for (BlockPosition origin : List.of(new BlockPosition(1024, -64, 1040), new BlockPosition(-32, 0, -16), new BlockPosition(0, 64, 0))) {
            assertEquals(BlockPlacer.general(plan, origin, MIN_Y), BlockPlacer.aligned(plan, origin, MIN_Y), "at " + origin);
            assertEquals(BlockPlacer.general(plan, origin, MIN_Y), BlockPlacer.writes(plan, origin, MIN_Y, SECTIONS));
        }
    }

    @Test
    void putsEveryBlockAtItsShiftedPositionAndKeepsTheVoids() {
        BlockPosition origin = new BlockPosition(1030, -55, -7);

        Map<BlockPosition, Integer> written = flatten(BlockPlacer.writes(plan, origin, MIN_Y, SECTIONS));

        int voids = 0;
        for (int y = 0; y < plan.size().sizeY(); y++) {
            for (int z = 0; z < plan.size().sizeZ(); z++) {
                for (int x = 0; x < plan.size().sizeX(); x++) {
                    int state = plan.stateAt(x, y, z);
                    BlockPosition target = new BlockPosition(origin.x() + x, origin.y() + y, origin.z() + z);
                    if (state == BlockPlan.VOID) {
                        voids++;
                        assertTrue(!written.containsKey(target), "void written at " + target);
                    } else {
                        assertEquals(state, written.get(target), "at " + target);
                    }
                }
            }
        }
        assertEquals(plan.size().volume() - voids, written.size());
    }

    @Test
    void writesOnlyTheChunksAndSectionsThePlanTouches() {
        BlockPlan single = BlockPlan.ofStates(new PlanSize(1, 1, 1), new int[] {FakeBlockNames.GOLD}, state -> state == FakeBlockNames.AIR);

        List<ChunkWrite> writes = BlockPlacer.writes(single, new BlockPosition(17, -49, -1), MIN_Y, SECTIONS);

        assertEquals(1, writes.size());
        assertEquals(new ChunkKey(1, -1), writes.getFirst().chunk());
        assertEquals(1, writes.getFirst().sections().size());
        SectionWrite section = writes.getFirst().sections().getFirst();
        assertEquals(0, section.sectionIndex());
        assertEquals(FakeBlockNames.GOLD, section.stateAt(SectionWrite.blockIndex(1, 15, 15)));
        assertEquals(SectionWrite.KEEP, section.stateAt(SectionWrite.blockIndex(0, 15, 15)));
    }

    @Test
    void refusesAPlanLeavingTheHeightOfTheWorld() {
        assertThrows(IllegalArgumentException.class, () -> BlockPlacer.writes(plan, new BlockPosition(0, -65, 0), MIN_Y, SECTIONS));
        assertThrows(IllegalArgumentException.class, () -> BlockPlacer.writes(plan, new BlockPosition(0, 301, 0), MIN_Y, SECTIONS));
        assertEquals(false, BlockPlacer.writes(plan, new BlockPosition(0, 300, 0), MIN_Y, SECTIONS).isEmpty());
    }

    @Test
    void capturesTheRealBlocksOfABoxIntoAPlan() {
        BlockBox box = new BlockBox(new BlockPosition(10, -60, 20), new BlockPosition(12, -59, 20));
        int[] real = {FakeBlockNames.STONE, FakeBlockNames.AIR, FakeBlockNames.GOLD, FakeBlockNames.GOLD, FakeBlockNames.STONE, FakeBlockNames.STAIRS};
        PlanService service = new PlanService(new FakeBlockNames(), (world, read) -> read.equals(box) ? real.clone() : new int[0],
                new PlanSettings(6, 1024));

        BlockPlan captured = service.capture(UUID.randomUUID(), box);

        assertEquals(new PlanSize(3, 2, 1), captured.size());
        assertEquals(5, captured.blockCount());
        assertEquals(4, captured.paletteSize());
        assertEquals(FakeBlockNames.STAIRS, captured.stateAt(2, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> service.capture(UUID.randomUUID(),
                new BlockBox(new BlockPosition(0, 0, 0), new BlockPosition(6, 0, 0))));
    }

    @Test
    void packsTheIndicesOfAPlanInTheFewestBits() {
        BlockPlan two = BlockPlan.ofStates(new PlanSize(64, 1, 1), new int[64], state -> state == 0);
        BlockPlan many = sample(new PlanSize(64, 1, 1));

        assertEquals(1 * 4 + 8, two.bytes());
        assertEquals(0, two.blockCount());
        assertTrue(many.bytes() > two.bytes());
        assertThrows(IllegalArgumentException.class, () -> BlockPlan.of(new PlanSize(1, 1, 1), new int[] {1}, new int[] {1}, state -> false));
        assertThrows(IllegalArgumentException.class, () -> BlockPlan.of(new PlanSize(2, 1, 1), new int[] {1}, new int[] {0}, state -> false));
    }

    private static BlockPlan sample(PlanSize size) {
        int[] palette = {BlockPlan.VOID, FakeBlockNames.AIR, FakeBlockNames.STONE, FakeBlockNames.STAIRS, FakeBlockNames.GOLD,
                FakeBlockNames.NUMBERED, FakeBlockNames.NUMBERED + 1};
        int[] indices = new int[(int) size.volume()];
        for (int index = 0; index < indices.length; index++) {
            indices[index] = (index * 7 + index / 13) % palette.length;
        }
        return BlockPlan.of(size, palette, indices, state -> state == FakeBlockNames.AIR);
    }

    private static Map<BlockPosition, Integer> flatten(List<ChunkWrite> writes) {
        Map<BlockPosition, Integer> blocks = new HashMap<>();
        for (ChunkWrite write : writes) {
            for (SectionWrite section : write.sections()) {
                for (int index = 0; index < SectionWrite.BLOCKS; index++) {
                    int state = section.stateAt(index);
                    if (state != SectionWrite.KEEP) {
                        BlockPosition position = new BlockPosition(write.chunk().x() * 16 + (index & 15),
                                MIN_Y + section.sectionIndex() * 16 + (index >> 8), write.chunk().z() * 16 + (index >> 4 & 15));
                        assertTrue(blocks.put(position, state) == null, "written twice at " + position);
                    }
                }
            }
        }
        return blocks;
    }
}

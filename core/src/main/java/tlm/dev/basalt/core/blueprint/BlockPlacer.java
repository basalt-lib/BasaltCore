package tlm.dev.basalt.core.blueprint;

import tlm.dev.basalt.core.edit.ChunkWrite;
import tlm.dev.basalt.core.edit.SectionWrite;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class BlockPlacer {

    private static final Comparator<ChunkKey> CHUNK_ORDER = Comparator.comparingInt(ChunkKey::x).thenComparingInt(ChunkKey::z);

    private BlockPlacer() {
    }

    public static List<ChunkWrite> writes(BlockPlan plan, BlockPosition origin, int minY, int sectionCount) {
        PlanSize size = plan.size();
        if (origin.y() < minY || (long) origin.y() + size.sizeY() > minY + sectionCount * 16L) {
            throw new IllegalArgumentException("a plan " + size.sizeY() + " high at y " + origin.y() + " leaves the height "
                    + minY + " to " + (minY + sectionCount * 16 - 1));
        }
        boolean aligned = Math.floorMod(origin.x(), 16) == 0 && Math.floorMod(origin.z(), 16) == 0
                && Math.floorMod(origin.y() - minY, 16) == 0;
        return aligned ? aligned(plan, origin, minY) : general(plan, origin, minY);
    }

    static List<ChunkWrite> general(BlockPlan plan, BlockPosition origin, int minY) {
        PlanSize size = plan.size();
        Map<ChunkKey, Map<Integer, int[]>> sections = new TreeMap<>(CHUNK_ORDER);
        int index = 0;
        for (int y = 0; y < size.sizeY(); y++) {
            int worldY = origin.y() + y - minY;
            int sectionIndex = worldY >> 4;
            int localY = worldY & 15;
            for (int z = 0; z < size.sizeZ(); z++) {
                int worldZ = origin.z() + z;
                int chunkZ = worldZ >> 4;
                int localZ = worldZ & 15;
                int[] states = null;
                int currentChunkX = Integer.MIN_VALUE;
                for (int x = 0; x < size.sizeX(); x++, index++) {
                    int state = plan.stateAt(index);
                    if (state == BlockPlan.VOID) {
                        continue;
                    }
                    int worldX = origin.x() + x;
                    int chunkX = worldX >> 4;
                    if (states == null || chunkX != currentChunkX) {
                        states = section(sections, new ChunkKey(chunkX, chunkZ), sectionIndex);
                        currentChunkX = chunkX;
                    }
                    states[SectionWrite.blockIndex(worldX & 15, localY, localZ)] = state;
                }
            }
        }
        return writes(sections);
    }

    static List<ChunkWrite> aligned(BlockPlan plan, BlockPosition origin, int minY) {
        PlanSize size = plan.size();
        Map<ChunkKey, Map<Integer, int[]>> sections = new TreeMap<>(CHUNK_ORDER);
        int firstChunkX = origin.x() >> 4;
        int firstChunkZ = origin.z() >> 4;
        int firstSection = (origin.y() - minY) >> 4;
        for (int cubeY = 0; cubeY * 16 < size.sizeY(); cubeY++) {
            for (int cubeZ = 0; cubeZ * 16 < size.sizeZ(); cubeZ++) {
                for (int cubeX = 0; cubeX * 16 < size.sizeX(); cubeX++) {
                    int[] states = null;
                    int toY = Math.min(16, size.sizeY() - cubeY * 16);
                    int toZ = Math.min(16, size.sizeZ() - cubeZ * 16);
                    int toX = Math.min(16, size.sizeX() - cubeX * 16);
                    for (int localY = 0; localY < toY; localY++) {
                        for (int localZ = 0; localZ < toZ; localZ++) {
                            int row = size.index(cubeX * 16, cubeY * 16 + localY, cubeZ * 16 + localZ);
                            for (int localX = 0; localX < toX; localX++) {
                                int state = plan.stateAt(row + localX);
                                if (state == BlockPlan.VOID) {
                                    continue;
                                }
                                if (states == null) {
                                    states = section(sections, new ChunkKey(firstChunkX + cubeX, firstChunkZ + cubeZ), firstSection + cubeY);
                                }
                                states[SectionWrite.blockIndex(localX, localY, localZ)] = state;
                            }
                        }
                    }
                }
            }
        }
        return writes(sections);
    }

    private static int[] section(Map<ChunkKey, Map<Integer, int[]>> sections, ChunkKey chunk, int sectionIndex) {
        return sections.computeIfAbsent(chunk, key -> new TreeMap<>()).computeIfAbsent(sectionIndex, index -> {
            int[] states = new int[SectionWrite.BLOCKS];
            Arrays.fill(states, SectionWrite.KEEP);
            return states;
        });
    }

    private static List<ChunkWrite> writes(Map<ChunkKey, Map<Integer, int[]>> sections) {
        List<ChunkWrite> writes = new ArrayList<>(sections.size());
        sections.forEach((chunk, byIndex) -> {
            List<SectionWrite> written = new ArrayList<>(byIndex.size());
            byIndex.forEach((index, states) -> written.add(new SectionWrite(index, states)));
            writes.add(new ChunkWrite(chunk, written));
        });
        return writes;
    }
}

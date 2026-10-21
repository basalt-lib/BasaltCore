package tlm.dev.basalt.core.chunk;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class VirtualChunk {

    private final ChunkKey key;
    private final ChunkShape shape;
    private final List<ChunkSection> sections;
    private final Map<HeightmapType, int[]> heights;

    VirtualChunk(ChunkKey key, ChunkShape shape, List<ChunkSection> sections, Map<HeightmapType, int[]> heights) {
        if (sections.size() != shape.sectionCount()) {
            throw new IllegalArgumentException("expected " + shape.sectionCount() + " sections, got " + sections.size());
        }
        this.key = key;
        this.shape = shape;
        this.sections = List.copyOf(sections);
        this.heights = copy(heights);
    }

    public ChunkKey key() {
        return key;
    }

    public ChunkShape shape() {
        return shape;
    }

    public List<ChunkSection> sections() {
        return sections;
    }

    public Map<HeightmapType, int[]> heights() {
        return copy(heights);
    }

    public int stateAt(int x, int y, int z) {
        return sections.get(shape.sectionIndex(y)).stateAt(x, (y - shape.minY()) & 15, z);
    }

    public int nonAirCount() {
        return sections.stream().mapToInt(ChunkSection::nonAirCount).sum();
    }

    public boolean holds(int x, int y, int z) {
        return x >= 0 && x <= 15 && z >= 0 && z <= 15 && shape.containsY(y);
    }

    public VirtualChunk withBlock(int x, int y, int z, int state, BlockStateTraits traits) {
        if (state < 0) {
            throw new IllegalArgumentException("negative state " + state);
        }
        int sectionIndex = shape.sectionIndex(y);
        int localY = (y - shape.minY()) & 15;
        ChunkSection section = sections.get(sectionIndex);
        int previous = section.stateAt(x, localY, z);
        if (previous == state) {
            return this;
        }
        int[] states = section.states().toArray();
        states[ChunkSection.blockIndex(x, localY, z)] = state;
        int nonAir = section.nonAirCount() + (traits.isAir(previous) ? 0 : -1) + (traits.isAir(state) ? 0 : 1);
        List<ChunkSection> changed = new ArrayList<>(sections);
        changed.set(sectionIndex, new ChunkSection(PalettedValues.of(states, section.states().format()), section.biomes(), nonAir));
        Map<HeightmapType, int[]> changedHeights = copy(heights);
        for (HeightmapType type : HeightmapType.values()) {
            changedHeights.get(type)[HeightmapCodec.column(x, z)] = columnHeight(changed, x, z, type, traits);
        }
        return new VirtualChunk(key, shape, changed, changedHeights);
    }

    private static int columnHeight(List<ChunkSection> sections, int x, int z, HeightmapType type, BlockStateTraits traits) {
        for (int sectionIndex = sections.size() - 1; sectionIndex >= 0; sectionIndex--) {
            ChunkSection section = sections.get(sectionIndex);
            if (section.nonAirCount() == 0) {
                continue;
            }
            for (int localY = 15; localY >= 0; localY--) {
                if (traits.countsIn(type, section.stateAt(x, localY, z))) {
                    return sectionIndex * 16 + localY + 1;
                }
            }
        }
        return 0;
    }

    private static Map<HeightmapType, int[]> copy(Map<HeightmapType, int[]> heights) {
        Map<HeightmapType, int[]> copy = new EnumMap<>(HeightmapType.class);
        heights.forEach((type, values) -> copy.put(type, values.clone()));
        return copy;
    }
}

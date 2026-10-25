package tlm.dev.basalt.core.chunk;

import tlm.dev.basalt.core.edit.SectionWrite;
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
        return filled(new LocalBox(x, y, z, x, y, z), state, traits);
    }

    public VirtualChunk filled(LocalBox box, int state, BlockStateTraits traits) {
        if (state < 0) {
            throw new IllegalArgumentException("negative state " + state);
        }
        if (!shape.containsY(box.minY()) || !shape.containsY(box.maxY())) {
            throw new IndexOutOfBoundsException("y " + box.minY() + " to " + box.maxY() + " outside the chunk");
        }
        List<ChunkSection> changed = new ArrayList<>(sections);
        boolean any = false;
        for (int sectionIndex = shape.sectionIndex(box.minY()); sectionIndex <= shape.sectionIndex(box.maxY()); sectionIndex++) {
            ChunkSection filled = fillSection(sectionIndex, box, state, traits);
            if (filled != sections.get(sectionIndex)) {
                changed.set(sectionIndex, filled);
                any = true;
            }
        }
        if (!any) {
            return this;
        }
        Map<HeightmapType, int[]> changedHeights = copy(heights);
        for (int z = box.minZ(); z <= box.maxZ(); z++) {
            for (int x = box.minX(); x <= box.maxX(); x++) {
                for (HeightmapType type : HeightmapType.values()) {
                    changedHeights.get(type)[HeightmapCodec.column(x, z)] = columnHeight(changed, x, z, type, traits);
                }
            }
        }
        return new VirtualChunk(key, shape, changed, changedHeights);
    }

    public VirtualChunk written(List<SectionWrite> writes, BlockStateTraits traits) {
        List<ChunkSection> changed = new ArrayList<>(sections);
        boolean any = false;
        for (SectionWrite write : writes) {
            if (write.sectionIndex() >= sections.size()) {
                throw new IndexOutOfBoundsException("section " + write.sectionIndex() + " outside a chunk of " + sections.size());
            }
            ChunkSection section = changed.get(write.sectionIndex());
            ChunkSection writtenSection = writeSection(section, write, traits);
            if (writtenSection != section) {
                changed.set(write.sectionIndex(), writtenSection);
                any = true;
            }
        }
        if (!any) {
            return this;
        }
        Map<HeightmapType, int[]> changedHeights = copy(heights);
        for (int z = 0; z < 16; z++) {
            for (int x = 0; x < 16; x++) {
                for (HeightmapType type : HeightmapType.values()) {
                    changedHeights.get(type)[HeightmapCodec.column(x, z)] = columnHeight(changed, x, z, type, traits);
                }
            }
        }
        return new VirtualChunk(key, shape, changed, changedHeights);
    }

    private static ChunkSection writeSection(ChunkSection section, SectionWrite write, BlockStateTraits traits) {
        int[] states = null;
        int nonAir = section.nonAirCount();
        for (int index = 0; index < SectionWrite.BLOCKS; index++) {
            int state = write.stateAt(index);
            if (state == SectionWrite.KEEP) {
                continue;
            }
            int previous = states == null ? section.states().get(index) : states[index];
            if (previous == state) {
                continue;
            }
            if (states == null) {
                states = section.states().toArray();
            }
            states[index] = state;
            nonAir += (traits.isAir(previous) ? 0 : -1) + (traits.isAir(state) ? 0 : 1);
        }
        if (states == null) {
            return section;
        }
        return new ChunkSection(PalettedValues.of(states, section.states().format()), section.biomes(), nonAir);
    }

    private ChunkSection fillSection(int sectionIndex, LocalBox box, int state, BlockStateTraits traits) {
        ChunkSection section = sections.get(sectionIndex);
        int sectionMinY = shape.minY() + sectionIndex * 16;
        int fromY = Math.max(box.minY(), sectionMinY) - sectionMinY;
        int toY = Math.min(box.maxY(), sectionMinY + 15) - sectionMinY;
        boolean placesAir = traits.isAir(state);
        int[] states = null;
        int nonAir = section.nonAirCount();
        for (int y = fromY; y <= toY; y++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                for (int x = box.minX(); x <= box.maxX(); x++) {
                    int index = ChunkSection.blockIndex(x, y, z);
                    int previous = states == null ? section.states().get(index) : states[index];
                    if (previous == state) {
                        continue;
                    }
                    if (states == null) {
                        states = section.states().toArray();
                    }
                    states[index] = state;
                    nonAir += (traits.isAir(previous) ? 0 : -1) + (placesAir ? 0 : 1);
                }
            }
        }
        if (states == null) {
            return section;
        }
        return new ChunkSection(PalettedValues.of(states, section.states().format()), section.biomes(), nonAir);
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

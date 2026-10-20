package tlm.dev.basalt.core.chunk;

import tlm.dev.basalt.core.position.ChunkKey;

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

    private static Map<HeightmapType, int[]> copy(Map<HeightmapType, int[]> heights) {
        Map<HeightmapType, int[]> copy = new EnumMap<>(HeightmapType.class);
        heights.forEach((type, values) -> copy.put(type, values.clone()));
        return copy;
    }
}

package tlm.dev.basalt.core.chunk;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class VirtualChunkBuilder {

    private final ChunkKey key;
    private final ChunkShape shape;
    private final int airState;
    private final int[][] sections;

    public VirtualChunkBuilder(ChunkKey key, ChunkShape shape, int airState) {
        if (airState < 0) {
            throw new IllegalArgumentException("negative air state " + airState);
        }
        this.key = key;
        this.shape = shape;
        this.airState = airState;
        this.sections = new int[shape.sectionCount()][];
    }

    public ChunkShape shape() {
        return shape;
    }

    public VirtualChunkBuilder set(int x, int y, int z, int state) {
        if (state < 0) {
            throw new IllegalArgumentException("negative state " + state);
        }
        int sectionIndex = shape.sectionIndex(y);
        int blockIndex = ChunkSection.blockIndex(x, (y - shape.minY()) & 15, z);
        int[] states = sections[sectionIndex];
        if (states == null) {
            if (state == airState) {
                return this;
            }
            states = airFilled();
            sections[sectionIndex] = states;
        }
        states[blockIndex] = state;
        return this;
    }

    public VirtualChunk build(ChunkFormats formats, BlockStateTraits traits, int biome) {
        int[] biomes = new int[ContainerFormat.SECTION_BIOME_CELLS];
        Arrays.fill(biomes, biome);
        PalettedValues biomeValues = PalettedValues.of(biomes, formats.biomes());
        ChunkSection emptySection = new ChunkSection(PalettedValues.of(airFilled(), formats.blockStates()), biomeValues, 0);
        List<ChunkSection> built = new ArrayList<>(sections.length);
        for (int[] states : sections) {
            built.add(states == null ? emptySection : new ChunkSection(
                    PalettedValues.of(states, formats.blockStates()), biomeValues, nonAirCount(states, traits)));
        }
        return new VirtualChunk(key, shape, built, heights(traits));
    }

    private Map<HeightmapType, int[]> heights(BlockStateTraits traits) {
        Map<HeightmapType, int[]> heights = new EnumMap<>(HeightmapType.class);
        for (HeightmapType type : HeightmapType.values()) {
            int[] values = new int[HeightmapCodec.COLUMNS];
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    values[HeightmapCodec.column(x, z)] = columnHeight(x, z, type, traits);
                }
            }
            heights.put(type, values);
        }
        return heights;
    }

    private int columnHeight(int x, int z, HeightmapType type, BlockStateTraits traits) {
        for (int sectionIndex = sections.length - 1; sectionIndex >= 0; sectionIndex--) {
            int[] states = sections[sectionIndex];
            if (states == null) {
                continue;
            }
            for (int localY = 15; localY >= 0; localY--) {
                if (traits.countsIn(type, states[ChunkSection.blockIndex(x, localY, z)])) {
                    return sectionIndex * 16 + localY + 1;
                }
            }
        }
        return 0;
    }

    private static int nonAirCount(int[] states, BlockStateTraits traits) {
        int count = 0;
        for (int state : states) {
            if (!traits.isAir(state)) {
                count++;
            }
        }
        return count;
    }

    private int[] airFilled() {
        int[] states = new int[ContainerFormat.SECTION_BLOCKS];
        Arrays.fill(states, airState);
        return states;
    }
}

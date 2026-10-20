package tlm.dev.basalt.core.chunk;

public record ChunkSection(PalettedValues states, PalettedValues biomes, int nonAirCount) {

    public ChunkSection {
        if (states.format().entryCount() != ContainerFormat.SECTION_BLOCKS) {
            throw new IllegalArgumentException("states must hold " + ContainerFormat.SECTION_BLOCKS + " blocks");
        }
        if (biomes.format().entryCount() != ContainerFormat.SECTION_BIOME_CELLS) {
            throw new IllegalArgumentException("biomes must hold " + ContainerFormat.SECTION_BIOME_CELLS + " cells");
        }
        if (nonAirCount < 0 || nonAirCount > ContainerFormat.SECTION_BLOCKS) {
            throw new IllegalArgumentException("non-air count out of range: " + nonAirCount);
        }
    }

    public int stateAt(int x, int y, int z) {
        return states.get(blockIndex(x, y, z));
    }

    public static int blockIndex(int x, int y, int z) {
        if ((x | y | z) < 0 || x > 15 || y > 15 || z > 15) {
            throw new IndexOutOfBoundsException("block " + x + ", " + y + ", " + z + " outside a section");
        }
        return (y << 4 | z) << 4 | x;
    }
}

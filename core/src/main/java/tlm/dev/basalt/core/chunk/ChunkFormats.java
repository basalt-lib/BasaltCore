package tlm.dev.basalt.core.chunk;

public record ChunkFormats(ContainerFormat blockStates, ContainerFormat biomes, SectionHeader header) {

    public ChunkFormats {
        if (blockStates.entryCount() != ContainerFormat.SECTION_BLOCKS) {
            throw new IllegalArgumentException("block states need " + ContainerFormat.SECTION_BLOCKS + " entries");
        }
        if (biomes.entryCount() != ContainerFormat.SECTION_BIOME_CELLS) {
            throw new IllegalArgumentException("biomes need " + ContainerFormat.SECTION_BIOME_CELLS + " entries");
        }
        if (header == null) {
            throw new IllegalArgumentException("the section header of the game version is missing");
        }
    }

    public static ChunkFormats of(int blockStateCount, int biomeCount, SectionHeader header) {
        return new ChunkFormats(
                ContainerFormat.blockStates(PackedLongs.ceilLog2(blockStateCount)),
                ContainerFormat.biomes(PackedLongs.ceilLog2(biomeCount)),
                header);
    }
}

package tlm.dev.basalt.core.chunk;

public record ContainerFormat(int entryCount, int minPaletteBits, int maxPaletteBits, int globalBits) {

    public static final int SECTION_BLOCKS = 4096;
    public static final int SECTION_BIOME_CELLS = 64;

    public ContainerFormat {
        if (entryCount < 1) {
            throw new IllegalArgumentException("entry count must be positive: " + entryCount);
        }
        if (minPaletteBits < 1 || maxPaletteBits < minPaletteBits || globalBits <= maxPaletteBits || globalBits > 31) {
            throw new IllegalArgumentException("invalid bit range " + minPaletteBits + ".." + maxPaletteBits + ", global " + globalBits);
        }
    }

    public static ContainerFormat blockStates(int globalBits) {
        return new ContainerFormat(SECTION_BLOCKS, 4, 8, globalBits);
    }

    public static ContainerFormat biomes(int globalBits) {
        return new ContainerFormat(SECTION_BIOME_CELLS, 1, 3, globalBits);
    }

    public int bitsFor(int paletteSize) {
        if (paletteSize < 1) {
            throw new IllegalArgumentException("palette size must be positive: " + paletteSize);
        }
        if (paletteSize == 1) {
            return 0;
        }
        int bits = Math.max(PackedLongs.ceilLog2(paletteSize), minPaletteBits);
        return bits <= maxPaletteBits ? bits : globalBits;
    }

    public boolean usesPalette(int bits) {
        return bits <= maxPaletteBits;
    }

    public int storageBitsFor(int writtenBits) {
        if (writtenBits == 0) {
            return 0;
        }
        if (writtenBits <= maxPaletteBits) {
            return Math.max(writtenBits, minPaletteBits);
        }
        return globalBits;
    }
}

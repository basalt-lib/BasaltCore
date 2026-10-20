package tlm.dev.basalt.core.chunk;

public final class SectionCodec {

    private final ChunkFormats formats;

    public SectionCodec(ChunkFormats formats) {
        this.formats = formats;
    }

    public void write(ChunkSection section, ByteWriter out) {
        out.writeShort(section.nonAirCount());
        writeContainer(section.states(), out);
        writeContainer(section.biomes(), out);
    }

    public ChunkSection read(ByteReader in) {
        int nonAirCount = in.readShort();
        if (nonAirCount < 0 || nonAirCount > ContainerFormat.SECTION_BLOCKS) {
            throw new MalformedChunkDataException("non-air count out of range: " + nonAirCount);
        }
        PalettedValues states = readContainer(in, formats.blockStates());
        PalettedValues biomes = readContainer(in, formats.biomes());
        return new ChunkSection(states, biomes, nonAirCount);
    }

    private static void writeContainer(PalettedValues values, ByteWriter out) {
        out.writeByte(values.bits());
        if (values.bits() == 0) {
            out.writeVarInt(values.paletteEntry(0));
        } else if (values.format().usesPalette(values.bits())) {
            out.writeVarInt(values.paletteSize());
            for (int index = 0; index < values.paletteSize(); index++) {
                out.writeVarInt(values.paletteEntry(index));
            }
        }
        for (long word : values.rawData()) {
            out.writeLong(word);
        }
    }

    private static PalettedValues readContainer(ByteReader in, ContainerFormat format) {
        int bits = in.readUnsignedByte();
        int[] palette = readPalette(in, format, bits);
        int storageBits = format.storageBitsFor(bits);
        int longCount = storageBits == 0 ? 0 : PackedLongs.longCount(format.entryCount(), storageBits);
        if (in.remaining() < longCount * 8) {
            throw new MalformedChunkDataException("needs " + longCount + " longs, " + in.remaining() + " bytes left");
        }
        long[] data = new long[longCount];
        for (int index = 0; index < longCount; index++) {
            data[index] = in.readLong();
        }
        return PalettedValues.decoded(format, bits, palette, data);
    }

    private static int[] readPalette(ByteReader in, ContainerFormat format, int bits) {
        if (bits == 0) {
            return new int[] {readId(in)};
        }
        if (!format.usesPalette(bits)) {
            return new int[0];
        }
        int size = in.readVarInt();
        int capacity = 1 << format.storageBitsFor(bits);
        if (size < 1 || size > capacity) {
            throw new MalformedChunkDataException("palette of " + size + " entries for " + bits + " bits");
        }
        int[] palette = new int[size];
        for (int index = 0; index < size; index++) {
            palette[index] = readId(in);
        }
        return palette;
    }

    private static int readId(ByteReader in) {
        int id = in.readVarInt();
        if (id < 0) {
            throw new MalformedChunkDataException("negative registry id " + id);
        }
        return id;
    }
}

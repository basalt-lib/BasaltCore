package tlm.dev.basalt.core.chunk;

import java.util.Arrays;

public final class ChunkPacketWriter {

    public static final int LIGHT_ARRAY_BYTES = 2048;

    private static final byte FULL_SKY = (byte) 0xFF;

    private final SectionCodec sectionCodec;

    public ChunkPacketWriter(ChunkFormats formats) {
        this.sectionCodec = new SectionCodec(formats);
    }

    public byte[] write(VirtualChunk chunk) {
        ByteWriter sections = new ByteWriter(chunk.sections().size() * 16);
        for (ChunkSection section : chunk.sections()) {
            sectionCodec.write(section, sections);
        }
        int lightSections = chunk.shape().lightSectionCount();
        ByteWriter out = new ByteWriter(sections.size() + lightSections * (LIGHT_ARRAY_BYTES + 3) + 1024);
        out.writeInt(chunk.key().x());
        out.writeInt(chunk.key().z());
        new HeightmapCodec(chunk.shape()).write(chunk.heights(), out);
        out.writeVarInt(sections.size());
        out.writeBytes(sections.toByteArray());
        out.writeVarInt(0);
        writeFullSkyLight(lightSections, out);
        return out.toByteArray();
    }

    static long[] allSet(int bits) {
        long[] words = new long[(bits + 63) / 64];
        for (int bit = 0; bit < bits; bit++) {
            words[bit / 64] |= 1L << bit % 64;
        }
        return words;
    }

    private static void writeFullSkyLight(int lightSections, ByteWriter out) {
        long[] everySection = allSet(lightSections);
        writeBitSet(everySection, out);
        writeBitSet(new long[0], out);
        writeBitSet(new long[0], out);
        writeBitSet(everySection, out);
        byte[] fullSky = new byte[LIGHT_ARRAY_BYTES];
        Arrays.fill(fullSky, FULL_SKY);
        out.writeVarInt(lightSections);
        for (int section = 0; section < lightSections; section++) {
            out.writeVarInt(LIGHT_ARRAY_BYTES);
            out.writeBytes(fullSky);
        }
        out.writeVarInt(0);
    }

    private static void writeBitSet(long[] words, ByteWriter out) {
        out.writeVarInt(words.length);
        for (long word : words) {
            out.writeLong(word);
        }
    }
}

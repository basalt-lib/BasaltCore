package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ChunkPacketWriterTest {

    private static final ChunkPacketWriter WRITER = new ChunkPacketWriter(TestBlocks.FORMATS);

    @Test
    void writesThePositionThenTheHeightmaps() {
        VirtualChunk chunk = debugChunk(new ChunkKey(-3, 7), TestBlocks.OVERWORLD);
        ByteReader in = new ByteReader(WRITER.write(chunk));

        assertEquals(-3, in.readInt());
        assertEquals(7, in.readInt());
        Map<HeightmapType, int[]> heights = new HeightmapCodec(TestBlocks.OVERWORLD).read(in);
        for (HeightmapType type : HeightmapType.values()) {
            assertArrayEquals(chunk.heights().get(type), heights.get(type));
        }
    }

    @Test
    void writesEverySectionBehindItsByteLength() {
        VirtualChunk chunk = debugChunk(new ChunkKey(0, 0), TestBlocks.OVERWORLD);
        ByteReader in = afterHeightmaps(chunk);

        ByteReader sections = new ByteReader(in.readBytes(in.readVarInt()));
        SectionCodec codec = new SectionCodec(TestBlocks.FORMATS);
        for (int index = 0; index < 24; index++) {
            ChunkSection section = codec.read(sections);
            assertEquals(index == 0 ? 1087 : 0, section.nonAirCount());
            assertArrayEquals(chunk.sections().get(index).states().toArray(), section.states().toArray());
        }
        assertEquals(0, sections.remaining());
        assertEquals(0, in.readVarInt());
    }

    @Test
    void lightsEverySectionWithFullSkyAndNoBlockLight() {
        ByteReader in = afterSections(debugChunk(new ChunkKey(0, 0), TestBlocks.OVERWORLD));

        assertArrayEquals(new long[] {(1L << 26) - 1}, readBitSet(in));
        assertArrayEquals(new long[0], readBitSet(in));
        assertArrayEquals(new long[0], readBitSet(in));
        assertArrayEquals(new long[] {(1L << 26) - 1}, readBitSet(in));
        assertEquals(26, in.readVarInt());
        byte[] fullSky = new byte[2048];
        Arrays.fill(fullSky, (byte) 0xFF);
        for (int section = 0; section < 26; section++) {
            assertEquals(2048, in.readVarInt());
            assertArrayEquals(fullSky, in.readBytes(2048));
        }
        assertEquals(0, in.readVarInt());
        assertEquals(0, in.remaining());
    }

    @Test
    void spreadsLightMasksOverSeveralLongs() {
        assertArrayEquals(new long[] {-1L}, ChunkPacketWriter.allSet(64));
        assertArrayEquals(new long[] {-1L, 1L}, ChunkPacketWriter.allSet(65));
        assertArrayEquals(new long[] {3L}, ChunkPacketWriter.allSet(2));
    }

    @Test
    void lightsATallWorldWithTwoMaskWords() {
        ChunkShape tall = new ChunkShape(-64, 63);
        ByteReader in = afterSections(debugChunk(new ChunkKey(0, 0), tall));

        assertArrayEquals(new long[] {-1L, 1L}, readBitSet(in));
    }

    private static VirtualChunk debugChunk(ChunkKey key, ChunkShape shape) {
        VirtualChunkBuilder builder = new VirtualChunkBuilder(key, shape, TestBlocks.AIR);
        DebugChunkPattern.paint(builder, block -> TestBlocks.STONE);
        return builder.build(TestBlocks.FORMATS, new TestBlocks(), TestBlocks.PLAINS);
    }

    private static ByteReader afterHeightmaps(VirtualChunk chunk) {
        ByteReader in = new ByteReader(WRITER.write(chunk));
        in.readInt();
        in.readInt();
        new HeightmapCodec(chunk.shape()).read(in);
        return in;
    }

    private static ByteReader afterSections(VirtualChunk chunk) {
        ByteReader in = afterHeightmaps(chunk);
        in.readBytes(in.readVarInt());
        in.readVarInt();
        return in;
    }

    private static long[] readBitSet(ByteReader in) {
        long[] words = new long[in.readVarInt()];
        for (int index = 0; index < words.length; index++) {
            words[index] = in.readLong();
        }
        return words;
    }
}

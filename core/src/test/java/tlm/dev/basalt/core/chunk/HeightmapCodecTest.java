package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HeightmapCodecTest {

    private static final ChunkShape OVERWORLD = new ChunkShape(-64, 24);
    private static final HeightmapCodec CODEC = new HeightmapCodec(OVERWORLD);

    @Test
    void usesNineBitsAndThirtySevenLongsForTheOverworld() {
        assertEquals(9, OVERWORLD.heightmapBits());

        ByteReader in = new ByteReader(encode(Map.of(HeightmapType.MOTION_BLOCKING, filled(4))));

        assertEquals(1, in.readVarInt());
        assertEquals(4, in.readVarInt());
        assertEquals(37, in.readVarInt());
        assertEquals(37 * 8, in.remaining());
    }

    @Test
    void writesTheTypesInProtocolOrder() {
        Map<HeightmapType, int[]> heights = new EnumMap<>(HeightmapType.class);
        heights.put(HeightmapType.MOTION_BLOCKING_NO_LEAVES, filled(1));
        heights.put(HeightmapType.WORLD_SURFACE, filled(2));
        ByteReader in = new ByteReader(encode(heights));

        assertEquals(2, in.readVarInt());
        assertEquals(1, in.readVarInt());
        in.readBytes(in.readVarInt() * 8);
        assertEquals(5, in.readVarInt());
    }

    @Test
    void readsBackEveryColumn() {
        int[] values = new int[256];
        for (int column = 0; column < values.length; column++) {
            values[column] = column % OVERWORLD.height() + 128;
        }
        Map<HeightmapType, int[]> heights = Map.of(
                HeightmapType.WORLD_SURFACE, values, HeightmapType.MOTION_BLOCKING, filled(0), HeightmapType.MOTION_BLOCKING_NO_LEAVES, filled(384));

        Map<HeightmapType, int[]> read = CODEC.read(new ByteReader(encode(heights)));

        assertArrayEquals(values, read.get(HeightmapType.WORLD_SURFACE));
        assertArrayEquals(filled(0), read.get(HeightmapType.MOTION_BLOCKING));
        assertArrayEquals(filled(384), read.get(HeightmapType.MOTION_BLOCKING_NO_LEAVES));
    }

    @Test
    void readsAnEmptyMap() {
        assertEquals(Map.of(), CODEC.read(new ByteReader(encode(Map.of()))));
    }

    @Test
    void indexesColumnsByXThenZ() {
        assertEquals(0, HeightmapCodec.column(0, 0));
        assertEquals(15, HeightmapCodec.column(15, 0));
        assertEquals(16, HeightmapCodec.column(0, 1));
        assertEquals(255, HeightmapCodec.column(15, 15));
        assertThrows(IndexOutOfBoundsException.class, () -> HeightmapCodec.column(16, 0));
    }

    @Test
    void rejectsHeightsOutsideTheWorld() {
        assertThrows(IllegalArgumentException.class, () -> encode(Map.of(HeightmapType.WORLD_SURFACE, filled(385))));
        assertThrows(IllegalArgumentException.class, () -> encode(Map.of(HeightmapType.WORLD_SURFACE, new int[255])));
    }

    @Test
    void rejectsAnUnknownType() {
        ByteWriter out = new ByteWriter(8);
        out.writeVarInt(1);
        out.writeVarInt(2);

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsATypeSentTwice() {
        ByteWriter out = new ByteWriter(1024);
        out.writeVarInt(2);
        for (int entry = 0; entry < 2; entry++) {
            out.writeVarInt(1);
            out.writeVarInt(37);
            for (int index = 0; index < 37; index++) {
                out.writeLong(0L);
            }
        }

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsTheWrongNumberOfLongs() {
        ByteWriter out = new ByteWriter(8);
        out.writeVarInt(1);
        out.writeVarInt(1);
        out.writeVarInt(36);

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsAValueAboveTheWorld() {
        ByteWriter out = new ByteWriter(512);
        out.writeVarInt(1);
        out.writeVarInt(1);
        out.writeVarInt(37);
        out.writeLong(511L);
        for (int index = 1; index < 37; index++) {
            out.writeLong(0L);
        }

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(out.toByteArray())));
    }

    @Test
    void rejectsATooLargeCountAndATruncatedMap() {
        ByteWriter tooMany = new ByteWriter(8);
        tooMany.writeVarInt(4);
        byte[] whole = encode(Map.of(HeightmapType.WORLD_SURFACE, filled(3)));

        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(tooMany.toByteArray())));
        assertThrows(MalformedChunkDataException.class, () -> CODEC.read(new ByteReader(Arrays.copyOf(whole, whole.length - 3))));
    }

    private static int[] filled(int height) {
        int[] values = new int[256];
        Arrays.fill(values, height);
        return values;
    }

    private static byte[] encode(Map<HeightmapType, int[]> heights) {
        ByteWriter out = new ByteWriter(512);
        CODEC.write(heights, out);
        return out.toByteArray();
    }
}

package tlm.dev.basalt.core.chunk;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

public final class HeightmapCodec {

    public static final int COLUMNS = 256;

    private final ChunkShape shape;

    public HeightmapCodec(ChunkShape shape) {
        this.shape = shape;
    }

    public void write(Map<HeightmapType, int[]> heights, ByteWriter out) {
        out.writeVarInt(heights.size());
        for (HeightmapType type : HeightmapType.values()) {
            int[] values = heights.get(type);
            if (values != null) {
                checkHeights(type, values);
                long[] packed = PackedLongs.pack(values, shape.heightmapBits());
                out.writeVarInt(type.networkId());
                out.writeVarInt(packed.length);
                for (long word : packed) {
                    out.writeLong(word);
                }
            }
        }
    }

    public Map<HeightmapType, int[]> read(ByteReader in) {
        int count = in.readVarInt();
        if (count < 0 || count > HeightmapType.values().length) {
            throw new MalformedChunkDataException("heightmap count out of range: " + count);
        }
        Map<HeightmapType, int[]> heights = new EnumMap<>(HeightmapType.class);
        int expectedLongs = PackedLongs.longCount(COLUMNS, shape.heightmapBits());
        for (int entry = 0; entry < count; entry++) {
            int networkId = in.readVarInt();
            HeightmapType type = HeightmapType.byNetworkId(networkId)
                    .orElseThrow(() -> new MalformedChunkDataException("unknown heightmap type " + networkId));
            if (heights.containsKey(type)) {
                throw new MalformedChunkDataException("heightmap " + type + " sent twice");
            }
            int length = in.readVarInt();
            if (length != expectedLongs) {
                throw new MalformedChunkDataException("heightmap " + type + " has " + length + " longs, expected " + expectedLongs);
            }
            long[] packed = new long[length];
            for (int index = 0; index < length; index++) {
                packed[index] = in.readLong();
            }
            int[] values = new int[COLUMNS];
            for (int column = 0; column < COLUMNS; column++) {
                values[column] = PackedLongs.get(packed, shape.heightmapBits(), column);
                if (values[column] > shape.height()) {
                    throw new MalformedChunkDataException("heightmap " + type + " value " + values[column] + " above " + shape.height());
                }
            }
            heights.put(type, values);
        }
        return Collections.unmodifiableMap(heights);
    }

    public static int column(int x, int z) {
        if ((x | z) < 0 || x > 15 || z > 15) {
            throw new IndexOutOfBoundsException("column " + x + ", " + z + " outside a chunk");
        }
        return x + z * 16;
    }

    private void checkHeights(HeightmapType type, int[] values) {
        if (values.length != COLUMNS) {
            throw new IllegalArgumentException("heightmap " + type + " needs " + COLUMNS + " values, got " + values.length);
        }
        for (int value : values) {
            if (value < 0 || value > shape.height()) {
                throw new IllegalArgumentException("heightmap " + type + " value " + value + " outside 0.." + shape.height());
            }
        }
    }
}

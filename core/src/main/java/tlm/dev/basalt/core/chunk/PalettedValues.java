package tlm.dev.basalt.core.chunk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class PalettedValues {

    private final ContainerFormat format;
    private final int bits;
    private final int[] palette;
    private final long[] data;

    private PalettedValues(ContainerFormat format, int bits, int[] palette, long[] data) {
        this.format = format;
        this.bits = bits;
        this.palette = palette;
        this.data = data;
    }

    public static PalettedValues of(int[] values, ContainerFormat format) {
        if (values.length != format.entryCount()) {
            throw new IllegalArgumentException("expected " + format.entryCount() + " values, got " + values.length);
        }
        Map<Integer, Integer> indexes = new HashMap<>();
        int[] palette = new int[values.length];
        int[] paletteIndexes = new int[values.length];
        for (int index = 0; index < values.length; index++) {
            int value = values[index];
            if (value < 0) {
                throw new IllegalArgumentException("negative value " + value + " at " + index);
            }
            Integer known = indexes.get(value);
            if (known == null) {
                known = indexes.size();
                indexes.put(value, known);
                palette[known] = value;
            }
            paletteIndexes[index] = known;
        }
        int bits = format.bitsFor(indexes.size());
        if (bits == 0) {
            return new PalettedValues(format, 0, new int[] {values[0]}, new long[0]);
        }
        if (format.usesPalette(bits)) {
            int storageBits = format.storageBitsFor(bits);
            return new PalettedValues(format, bits, Arrays.copyOf(palette, indexes.size()), PackedLongs.pack(paletteIndexes, storageBits));
        }
        for (int value : values) {
            if (value >= 1 << bits) {
                throw new IllegalArgumentException("value " + value + " does not fit in " + bits + " bits");
            }
        }
        return new PalettedValues(format, bits, new int[0], PackedLongs.pack(values, bits));
    }

    static PalettedValues decoded(ContainerFormat format, int writtenBits, int[] palette, long[] data) {
        int storageBits = format.storageBitsFor(writtenBits);
        int expectedLongs = storageBits == 0 ? 0 : PackedLongs.longCount(format.entryCount(), storageBits);
        if (data.length != expectedLongs) {
            throw new MalformedChunkDataException("expected " + expectedLongs + " longs for " + writtenBits + " bits, got " + data.length);
        }
        PalettedValues values = new PalettedValues(format, writtenBits, palette.clone(), data.clone());
        if (writtenBits > 0 && format.usesPalette(writtenBits)) {
            for (int index = 0; index < format.entryCount(); index++) {
                int paletteIndex = values.paletteIndex(index);
                if (paletteIndex >= palette.length) {
                    throw new MalformedChunkDataException("palette index " + paletteIndex + " out of " + palette.length + " at " + index);
                }
            }
        }
        return values;
    }

    public int get(int index) {
        if (index < 0 || index >= format.entryCount()) {
            throw new IndexOutOfBoundsException("index " + index + " out of " + format.entryCount());
        }
        if (bits == 0) {
            return palette[0];
        }
        if (format.usesPalette(bits)) {
            return palette[paletteIndex(index)];
        }
        return PackedLongs.get(data, bits, index);
    }

    public int[] toArray() {
        int[] values = new int[format.entryCount()];
        for (int index = 0; index < values.length; index++) {
            values[index] = get(index);
        }
        return values;
    }

    public int bits() {
        return bits;
    }

    public int[] palette() {
        return palette.clone();
    }

    public long[] data() {
        return data.clone();
    }

    public ContainerFormat format() {
        return format;
    }

    int paletteSize() {
        return palette.length;
    }

    int paletteEntry(int index) {
        return palette[index];
    }

    long[] rawData() {
        return data;
    }

    private int paletteIndex(int index) {
        return PackedLongs.get(data, format.storageBitsFor(bits), index);
    }
}

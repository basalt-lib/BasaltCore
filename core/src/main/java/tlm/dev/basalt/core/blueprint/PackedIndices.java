package tlm.dev.basalt.core.blueprint;

final class PackedIndices {

    private final int bits;
    private final int perLong;
    private final long mask;
    private final long[] data;

    private PackedIndices(int bits, long[] data) {
        this.bits = bits;
        this.perLong = Long.SIZE / bits;
        this.mask = (1L << bits) - 1;
        this.data = data;
    }

    static PackedIndices of(int[] values, int maxValue) {
        int bits = Math.max(1, Integer.SIZE - Integer.numberOfLeadingZeros(maxValue));
        int perLong = Long.SIZE / bits;
        long[] data = new long[(values.length + perLong - 1) / perLong];
        for (int index = 0; index < values.length; index++) {
            int value = values[index];
            if (value < 0 || value > maxValue) {
                throw new IllegalArgumentException("index " + value + " outside 0 to " + maxValue);
            }
            data[index / perLong] |= (long) value << (index % perLong) * bits;
        }
        return new PackedIndices(bits, data);
    }

    int get(int index) {
        return (int) (data[index / perLong] >>> (index % perLong) * bits & mask);
    }

    int bits() {
        return bits;
    }

    long bytes() {
        return (long) data.length * Long.BYTES;
    }
}

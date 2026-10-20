package tlm.dev.basalt.core.chunk;

final class PackedLongs {

    private PackedLongs() {
    }

    static int longCount(int size, int bits) {
        int valuesPerLong = 64 / bits;
        return (size + valuesPerLong - 1) / valuesPerLong;
    }

    static long[] pack(int[] values, int bits) {
        int valuesPerLong = 64 / bits;
        long[] packed = new long[longCount(values.length, bits)];
        for (int index = 0; index < values.length; index++) {
            int shift = index % valuesPerLong * bits;
            packed[index / valuesPerLong] |= (values[index] & mask(bits)) << shift;
        }
        return packed;
    }

    static int get(long[] packed, int bits, int index) {
        int valuesPerLong = 64 / bits;
        int shift = index % valuesPerLong * bits;
        return (int) (packed[index / valuesPerLong] >>> shift & mask(bits));
    }

    static int ceilLog2(int value) {
        return value <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(value - 1);
    }

    private static long mask(int bits) {
        return (1L << bits) - 1L;
    }
}

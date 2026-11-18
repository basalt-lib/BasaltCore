package tlm.dev.basalt.core.overlay;

import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;

public final class OverlayBlockIndex {

    public static final int NONE = -1;
    public static final int MIN_Y = -2048;
    public static final int MAX_Y = 2047;

    private static final OverlayBlockIndex EMPTY = new OverlayBlockIndex(new long[0], new int[0]);
    private static final int CHUNK_BITS = 22;
    private static final long CHUNK_MASK = (1L << CHUNK_BITS) - 1;
    private static final int CHUNK_X_SHIFT = 42;
    private static final int CHUNK_Z_SHIFT = 20;
    private static final int Y_SHIFT = 8;
    private static final long LOW_BITS = (1L << CHUNK_Z_SHIFT) - 1;

    private final long[] keys;
    private final int[] states;

    private OverlayBlockIndex(long[] keys, int[] states) {
        this.keys = keys;
        this.states = states;
    }

    public static OverlayBlockIndex empty() {
        return EMPTY;
    }

    public static OverlayBlockIndex of(NavigableMap<Long, Integer> blocks) {
        if (blocks.isEmpty()) {
            return EMPTY;
        }
        long[] keys = new long[blocks.size()];
        int[] states = new int[blocks.size()];
        int index = 0;
        for (Map.Entry<Long, Integer> block : blocks.entrySet()) {
            keys[index] = block.getKey();
            states[index] = block.getValue();
            index++;
        }
        return new OverlayBlockIndex(keys, states);
    }

    public static long key(int x, int y, int z) {
        if (y < MIN_Y || y > MAX_Y) {
            throw new IllegalArgumentException("y " + y + " outside " + MIN_Y + ".." + MAX_Y);
        }
        return chunkPrefix(x >> 4, z >> 4) | ((long) y & 0xFFF) << Y_SHIFT | (long) (z & 15) << 4 | (x & 15);
    }

    public int size() {
        return keys.length;
    }

    public int stateAt(int x, int y, int z) {
        if (y < MIN_Y || y > MAX_Y || keys.length == 0) {
            return NONE;
        }
        int found = Arrays.binarySearch(keys, key(x, y, z));
        return found < 0 ? NONE : states[found];
    }

    public int firstIn(int chunkX, int chunkZ) {
        return lowerBound(chunkPrefix(chunkX, chunkZ));
    }

    public int endIn(int chunkX, int chunkZ) {
        return lowerBound(chunkPrefix(chunkX, chunkZ) + LOW_BITS + 1);
    }

    public int firstInSection(int chunkX, int sectionY, int chunkZ) {
        return lowerBound(chunkPrefix(chunkX, chunkZ) | sectionBottom(sectionY));
    }

    public int endInSection(int chunkX, int sectionY, int chunkZ) {
        return lowerBound((chunkPrefix(chunkX, chunkZ) | sectionBottom(sectionY)) + (16L << Y_SHIFT));
    }

    public boolean holdsChunk(int chunkX, int chunkZ) {
        return firstIn(chunkX, chunkZ) < endIn(chunkX, chunkZ);
    }

    public int x(int index) {
        return (int) (keys[index] >> CHUNK_X_SHIFT) << 4 | (int) (keys[index] & 15);
    }

    public int y(int index) {
        return (int) (keys[index] << (64 - CHUNK_Z_SHIFT) >> (64 - 12));
    }

    public int z(int index) {
        return (int) (keys[index] << CHUNK_BITS >> CHUNK_X_SHIFT) << 4 | (int) (keys[index] >> 4 & 15);
    }

    public int state(int index) {
        return states[index];
    }

    private static long chunkPrefix(int chunkX, int chunkZ) {
        return (chunkX & CHUNK_MASK) << CHUNK_X_SHIFT | (chunkZ & CHUNK_MASK) << CHUNK_Z_SHIFT;
    }

    private static long sectionBottom(int sectionY) {
        return ((long) (sectionY << 4) & 0xFFF) << Y_SHIFT;
    }

    private int lowerBound(long key) {
        int low = 0;
        int high = keys.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (keys[middle] < key) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }
}

package tlm.dev.basalt.core.blueprint;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntPredicate;

public final class BlockPlan {

    public static final int VOID = -1;

    private final PlanSize size;
    private final int[] palette;
    private final PackedIndices indices;
    private final int blockCount;

    private BlockPlan(PlanSize size, int[] palette, PackedIndices indices, int blockCount) {
        this.size = size;
        this.palette = palette;
        this.indices = indices;
        this.blockCount = blockCount;
    }

    public static BlockPlan of(PlanSize size, int[] palette, int[] indices, IntPredicate isAir) {
        if (palette.length == 0) {
            throw new IllegalArgumentException("a plan needs at least one palette entry");
        }
        if (indices.length != size.volume()) {
            throw new IllegalArgumentException("a plan of " + size + " holds " + size.volume() + " indices, not " + indices.length);
        }
        for (int state : palette) {
            if (state < VOID) {
                throw new IllegalArgumentException("a palette state is " + VOID + " for a void or 0 or more: " + state);
            }
        }
        int blockCount = 0;
        for (int index : indices) {
            if (index < 0 || index >= palette.length) {
                throw new IllegalArgumentException("index " + index + " outside a palette of " + palette.length);
            }
            int state = palette[index];
            if (state != VOID && !isAir.test(state)) {
                blockCount++;
            }
        }
        return new BlockPlan(size, palette.clone(), PackedIndices.of(indices, palette.length - 1), blockCount);
    }

    public static BlockPlan ofStates(PlanSize size, int[] states, IntPredicate isAir) {
        Map<Integer, Integer> slots = new HashMap<>();
        int[] indices = new int[states.length];
        int[] palette = new int[Math.min(states.length, 16)];
        int last = Integer.MIN_VALUE;
        int lastSlot = 0;
        for (int index = 0; index < states.length; index++) {
            int state = states[index];
            if (state != last) {
                Integer slot = slots.get(state);
                if (slot == null) {
                    slot = slots.size();
                    slots.put(state, slot);
                    if (slot == palette.length) {
                        palette = Arrays.copyOf(palette, palette.length * 2);
                    }
                    palette[slot] = state;
                }
                last = state;
                lastSlot = slot;
            }
            indices[index] = lastSlot;
        }
        return of(size, Arrays.copyOf(palette, Math.max(1, slots.size())), indices, isAir);
    }

    public PlanSize size() {
        return size;
    }

    public int blockCount() {
        return blockCount;
    }

    public int paletteSize() {
        return palette.length;
    }

    public int stateAt(int index) {
        return palette[indices.get(index)];
    }

    public int stateAt(int x, int y, int z) {
        if (!size.contains(x, y, z)) {
            throw new IndexOutOfBoundsException(x + " " + y + " " + z + " outside a plan of " + size);
        }
        return stateAt(size.index(x, y, z));
    }

    public long bytes() {
        return (long) palette.length * Integer.BYTES + indices.bytes();
    }
}

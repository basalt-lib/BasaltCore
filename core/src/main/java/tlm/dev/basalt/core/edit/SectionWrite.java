package tlm.dev.basalt.core.edit;

import java.util.Arrays;

public final class SectionWrite {

    public static final int KEEP = -1;
    public static final int BLOCKS = 4096;

    private final int sectionIndex;
    private final int[] states;

    public SectionWrite(int sectionIndex, int[] states) {
        if (sectionIndex < 0) {
            throw new IllegalArgumentException("negative section index " + sectionIndex);
        }
        if (states.length != BLOCKS) {
            throw new IllegalArgumentException("a section write holds " + BLOCKS + " states, not " + states.length);
        }
        for (int state : states) {
            if (state < KEEP) {
                throw new IllegalArgumentException("a written state is " + KEEP + " to keep the block or 0 or more: " + state);
            }
        }
        this.sectionIndex = sectionIndex;
        this.states = states.clone();
    }

    public static int blockIndex(int x, int y, int z) {
        return (y << 4 | z) << 4 | x;
    }

    public int sectionIndex() {
        return sectionIndex;
    }

    public int stateAt(int blockIndex) {
        return states[blockIndex];
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SectionWrite write && write.sectionIndex == sectionIndex && Arrays.equals(write.states, states);
    }

    @Override
    public int hashCode() {
        return 31 * sectionIndex + Arrays.hashCode(states);
    }

    @Override
    public String toString() {
        return "SectionWrite[" + sectionIndex + "]";
    }
}

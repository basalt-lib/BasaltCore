package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PalettedValuesTest {

    private static final ContainerFormat BLOCKS = ContainerFormat.blockStates(15);

    @Test
    void storesASingleValueWithoutData() {
        int[] values = new int[4096];
        Arrays.fill(values, 7);

        PalettedValues packed = PalettedValues.of(values, BLOCKS);

        assertEquals(0, packed.bits());
        assertArrayEquals(new int[] {7}, packed.palette());
        assertEquals(0, packed.data().length);
        assertEquals(7, packed.get(4095));
    }

    @Test
    void keepsThePaletteInFirstSeenOrder() {
        int[] values = new int[4096];
        values[1] = 30;
        values[2] = 20;
        values[3] = 30;

        PalettedValues packed = PalettedValues.of(values, BLOCKS);

        assertArrayEquals(new int[] {0, 30, 20}, packed.palette());
        assertEquals(4, packed.bits());
        assertEquals(256, packed.data().length);
        assertArrayEquals(values, packed.toArray());
    }

    @Test
    void packsIndexesWithoutSpanningLongs() {
        int[] values = new int[4096];
        for (int index = 0; index < values.length; index++) {
            values[index] = index % 20;
        }

        PalettedValues packed = PalettedValues.of(values, BLOCKS);

        assertEquals(5, packed.bits());
        assertEquals(342, packed.data().length);
        assertArrayEquals(values, packed.toArray());
    }

    @Test
    void switchesToGlobalIdsForAFullBitArray() {
        int[] values = new int[4096];
        for (int index = 0; index < values.length; index++) {
            values[index] = 32767 - index;
        }

        PalettedValues packed = PalettedValues.of(values, BLOCKS);

        assertEquals(15, packed.bits());
        assertEquals(0, packed.palette().length);
        assertEquals(1024, packed.data().length);
        assertArrayEquals(values, packed.toArray());
    }

    @Test
    void returnsCopiesOfItsArrays() {
        int[] values = new int[4096];
        values[0] = 1;
        PalettedValues packed = PalettedValues.of(values, BLOCKS);

        packed.palette()[0] = 99;
        packed.data()[0] = -1L;

        assertEquals(1, packed.get(0));
    }

    @Test
    void rejectsTheWrongNumberOfValues() {
        assertThrows(IllegalArgumentException.class, () -> PalettedValues.of(new int[4095], BLOCKS));
    }

    @Test
    void rejectsNegativeValues() {
        int[] values = new int[4096];
        values[10] = -1;

        assertThrows(IllegalArgumentException.class, () -> PalettedValues.of(values, BLOCKS));
    }

    @Test
    void rejectsGlobalIdsTooLargeForTheRegistry() {
        int[] values = new int[4096];
        for (int index = 0; index < values.length; index++) {
            values[index] = index;
        }
        values[0] = 1 << 15;

        assertThrows(IllegalArgumentException.class, () -> PalettedValues.of(values, BLOCKS));
    }

    @Test
    void rejectsOutOfRangeIndexes() {
        PalettedValues packed = PalettedValues.of(new int[4096], BLOCKS);

        assertThrows(IndexOutOfBoundsException.class, () -> packed.get(4096));
        assertThrows(IndexOutOfBoundsException.class, () -> packed.get(-1));
    }
}

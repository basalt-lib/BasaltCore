package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContainerFormatTest {

    private final ContainerFormat blocks = ContainerFormat.blockStates(15);
    private final ContainerFormat biomes = ContainerFormat.biomes(7);

    @Test
    void blockStatesUseFourBitsAtLeastThenHashPalettesThenTheGlobalPalette() {
        assertEquals(0, blocks.bitsFor(1));
        assertEquals(4, blocks.bitsFor(2));
        assertEquals(4, blocks.bitsFor(16));
        assertEquals(5, blocks.bitsFor(17));
        assertEquals(8, blocks.bitsFor(256));
        assertEquals(15, blocks.bitsFor(257));
    }

    @Test
    void biomesUseOneToThreeBitsThenTheGlobalPalette() {
        assertEquals(0, biomes.bitsFor(1));
        assertEquals(1, biomes.bitsFor(2));
        assertEquals(2, biomes.bitsFor(4));
        assertEquals(3, biomes.bitsFor(5));
        assertEquals(3, biomes.bitsFor(8));
        assertEquals(7, biomes.bitsFor(9));
    }

    @Test
    void readsSmallBitCountsWithTheMinimumStorage() {
        assertEquals(0, blocks.storageBitsFor(0));
        assertEquals(4, blocks.storageBitsFor(1));
        assertEquals(4, blocks.storageBitsFor(3));
        assertEquals(6, blocks.storageBitsFor(6));
        assertEquals(15, blocks.storageBitsFor(9));
        assertEquals(15, blocks.storageBitsFor(255));
    }

    @Test
    void knowsWhenAPaletteIsWritten() {
        assertTrue(blocks.usesPalette(0));
        assertTrue(blocks.usesPalette(8));
        assertFalse(blocks.usesPalette(9));
    }

    @Test
    void rejectsAnEmptyPalette() {
        assertThrows(IllegalArgumentException.class, () -> blocks.bitsFor(0));
    }

    @Test
    void rejectsGlobalBitsThatDoNotExceedThePaletteBits() {
        assertThrows(IllegalArgumentException.class, () -> ContainerFormat.blockStates(8));
        assertThrows(IllegalArgumentException.class, () -> ContainerFormat.biomes(32));
    }
}

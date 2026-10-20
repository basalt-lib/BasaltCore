package tlm.dev.basalt.core.chunk;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkShapeTest {

    private final ChunkShape overworld = new ChunkShape(-64, 24);

    @Test
    void coversTheOverworldHeight() {
        assertEquals(384, overworld.height());
        assertTrue(overworld.containsY(-64));
        assertTrue(overworld.containsY(319));
        assertFalse(overworld.containsY(320));
        assertFalse(overworld.containsY(-65));
    }

    @Test
    void findsTheSectionOfAHeight() {
        assertEquals(0, overworld.sectionIndex(-64));
        assertEquals(0, overworld.sectionIndex(-49));
        assertEquals(1, overworld.sectionIndex(-48));
        assertEquals(23, overworld.sectionIndex(319));
        assertThrows(IndexOutOfBoundsException.class, () -> overworld.sectionIndex(320));
    }

    @Test
    void countsLightSectionsAboveAndBelow() {
        assertEquals(26, overworld.lightSectionCount());
    }

    @Test
    void sizesHeightmapsFromTheHeight() {
        assertEquals(9, overworld.heightmapBits());
        assertEquals(5, new ChunkShape(0, 1).heightmapBits());
    }

    @Test
    void rejectsShapesMinecraftCannotHave() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkShape(-60, 24));
        assertThrows(IllegalArgumentException.class, () -> new ChunkShape(-64, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkShape(-2048, 1));
        assertThrows(IllegalArgumentException.class, () -> new ChunkShape(2016, 2));
    }
}

package tlm.dev.basalt.core.overlay;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayBlockIndexTest {

    @Test
    void readsBackEveryCoordinateOfEveryBlock() {
        int[][] positions = {
                {0, 0, 0}, {15, -64, 15}, {-1, -1, -1}, {-16, 319, -17}, {1029, -58, 1029},
                {29_999_999, 2047, -29_999_999}, {-29_999_999, -2048, 29_999_999}, {7, 100, -8}};
        TreeMap<Long, Integer> blocks = new TreeMap<>();
        for (int index = 0; index < positions.length; index++) {
            blocks.put(OverlayBlockIndex.key(positions[index][0], positions[index][1], positions[index][2]), index + 10);
        }

        OverlayBlockIndex index = OverlayBlockIndex.of(blocks);

        assertEquals(positions.length, index.size());
        List<String> read = new ArrayList<>();
        for (int at = 0; at < index.size(); at++) {
            read.add(index.x(at) + " " + index.y(at) + " " + index.z(at));
            assertEquals(index.state(at), index.stateAt(index.x(at), index.y(at), index.z(at)));
        }
        for (int[] position : positions) {
            assertTrue(read.contains(position[0] + " " + position[1] + " " + position[2]), position[0] + " " + position[1] + " " + position[2]);
        }
        assertEquals(14, index.stateAt(1029, -58, 1029));
        assertEquals(OverlayBlockIndex.NONE, index.stateAt(1029, -57, 1029));
        assertEquals(OverlayBlockIndex.NONE, index.stateAt(1029, 5000, 1029));
    }

    @Test
    void keepsTheBlocksOfAChunkTogether() {
        TreeMap<Long, Integer> blocks = new TreeMap<>();
        blocks.put(OverlayBlockIndex.key(-16, 70, -16), 1);
        blocks.put(OverlayBlockIndex.key(-1, -64, -1), 2);
        blocks.put(OverlayBlockIndex.key(-1, 64, -16), 3);
        blocks.put(OverlayBlockIndex.key(0, 64, 0), 4);
        blocks.put(OverlayBlockIndex.key(-17, 64, -1), 5);
        OverlayBlockIndex index = OverlayBlockIndex.of(blocks);

        assertEquals(List.of(3, 1, 2), statesIn(index, -1, -1));
        assertEquals(List.of(4), statesIn(index, 0, 0));
        assertEquals(List.of(5), statesIn(index, -2, -1));
        assertEquals(List.of(), statesIn(index, 0, -1));
        assertTrue(index.holdsChunk(-1, -1));
        assertFalse(index.holdsChunk(1, 0));
    }

    @Test
    void findsTheBlocksOfOneSection() {
        TreeMap<Long, Integer> blocks = new TreeMap<>();
        blocks.put(OverlayBlockIndex.key(3, -1, 4), 1);
        blocks.put(OverlayBlockIndex.key(3, -16, 4), 2);
        blocks.put(OverlayBlockIndex.key(3, -17, 4), 3);
        blocks.put(OverlayBlockIndex.key(3, 0, 4), 4);
        blocks.put(OverlayBlockIndex.key(3, 15, 4), 5);
        blocks.put(OverlayBlockIndex.key(19, 15, 4), 6);
        OverlayBlockIndex index = OverlayBlockIndex.of(blocks);

        assertEquals(List.of(2, 1), statesInSection(index, 0, -1, 0));
        assertEquals(List.of(3), statesInSection(index, 0, -2, 0));
        assertEquals(List.of(4, 5), statesInSection(index, 0, 0, 0));
        assertEquals(List.of(6), statesInSection(index, 1, 0, 0));
        assertEquals(List.of(), statesInSection(index, 0, 1, 0));
    }

    @Test
    void refusesAHeightOutsideTheKey() {
        assertThrows(IllegalArgumentException.class, () -> OverlayBlockIndex.key(0, 2048, 0));
        assertThrows(IllegalArgumentException.class, () -> OverlayBlockIndex.key(0, -2049, 0));
        assertEquals(OverlayBlockIndex.NONE, OverlayBlockIndex.empty().stateAt(0, 0, 0));
        assertEquals(0, OverlayBlockIndex.empty().size());
    }

    private static List<Integer> statesIn(OverlayBlockIndex index, int chunkX, int chunkZ) {
        List<Integer> states = new ArrayList<>();
        for (int at = index.firstIn(chunkX, chunkZ); at < index.endIn(chunkX, chunkZ); at++) {
            states.add(index.state(at));
        }
        return states;
    }

    private static List<Integer> statesInSection(OverlayBlockIndex index, int chunkX, int sectionY, int chunkZ) {
        List<Integer> states = new ArrayList<>();
        for (int at = index.firstInSection(chunkX, sectionY, chunkZ); at < index.endInSection(chunkX, sectionY, chunkZ); at++) {
            states.add(index.state(at));
        }
        return states;
    }
}

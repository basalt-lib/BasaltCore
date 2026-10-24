package tlm.dev.basalt.api;

import org.bukkit.World;
import org.bukkit.util.BoundingBox;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkAreaTest {

    private static final World WORLD = (World) Proxy.newProxyInstance(World.class.getClassLoader(), new Class<?>[] {World.class},
            (proxy, method, arguments) -> switch (method.getName()) {
                case "equals" -> proxy == arguments[0];
                case "hashCode" -> 7;
                case "toString" -> "test world";
                default -> throw new UnsupportedOperationException(method.getName());
            });

    @Test
    void coversTheChunksBetweenItsCorners() {
        ChunkArea area = new ChunkArea(WORLD, -2, 3, 1, 3);

        assertEquals(4, area.chunkCount());
    }

    @Test
    void refusesACornerAboveTheOther() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkArea(WORLD, 2, 0, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkArea(WORLD, 0, 5, 0, 4));
    }

    @Test
    void refusesChunksBeyondTheWorldLimit() {
        int limit = ChunkArea.MAX_CHUNK_COORDINATE;

        new ChunkArea(WORLD, -limit, -limit, limit, limit);
        assertThrows(IllegalArgumentException.class, () -> new ChunkArea(WORLD, -limit - 1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkArea(WORLD, 0, 0, 0, limit + 1));
    }

    @Test
    void refusesAMissingWorld() {
        assertThrows(NullPointerException.class, () -> new ChunkArea(null, 0, 0, 0, 0));
    }

    @Test
    void takesTheChunksUnderTheBlocksOfABox() {
        ChunkArea area = ChunkArea.ofBlocks(WORLD, new BoundingBox(0, -64, -16, 32, 320, 0));

        assertEquals(new ChunkArea(WORLD, 0, -1, 1, -1), area);
    }

    @Test
    void keepsAtLeastOneBlockOnEachAxis() {
        ChunkArea area = ChunkArea.ofBlocks(WORLD, new BoundingBox(17.5, 0, -0.5, 17.5, 0, -0.5));

        assertEquals(new ChunkArea(WORLD, 1, -1, 1, -1), area);
    }

    @Test
    void countsPartlyCoveredChunks() {
        ChunkArea area = ChunkArea.ofBlocks(WORLD, new BoundingBox(15.2, 0, 15.9, 16.1, 1, 47.01));

        assertEquals(new ChunkArea(WORLD, 0, 0, 1, 2), area);
    }

    @Test
    void refusesABoxBeyondTheWorldOrNotFinite() {
        assertThrows(IllegalArgumentException.class, () -> ChunkArea.ofBlocks(WORLD, new BoundingBox(0, 0, 0, 3.1E7, 1, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> ChunkArea.ofBlocks(WORLD, new BoundingBox(0, 0, 0, Double.POSITIVE_INFINITY, 1, 1)));
    }
}

package tlm.dev.basalt.core.position;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ChunkKeyTest {

    @Test
    void packsXInTheLowHalfAndZInTheHighHalf() {
        assertEquals(1L | 2L << 32, new ChunkKey(1, 2).packed());
    }

    @Test
    void packsNegativeCoordinatesWithoutSignSpill() {
        assertEquals(0xFFFFFFFFL, new ChunkKey(-1, 0).packed());
        assertEquals(0xFFFFFFFF00000000L, new ChunkKey(0, -1).packed());
    }

    @Test
    void findsTheChunkOfABlock() {
        assertEquals(new ChunkKey(-1, 1), ChunkKey.ofBlock(-1, 17));
        assertEquals(new ChunkKey(0, 0), ChunkKey.ofBlock(15, 0));
    }

    @Test
    void findsTheChunkHoldingAPoint() {
        assertEquals(Optional.of(new ChunkKey(64, 64)), ChunkKey.containing(1024.0, 1039.99));
        assertEquals(Optional.of(new ChunkKey(-1, -1)), ChunkKey.containing(-0.01, -16.0));
        assertEquals(Optional.of(new ChunkKey(-2, 0)), ChunkKey.containing(-16.5, 15.999));
    }

    @Test
    void findsNoChunkForAPointOutsideTheWorldOrNotFinite() {
        assertEquals(Optional.empty(), ChunkKey.containing(Double.NaN, 0));
        assertEquals(Optional.empty(), ChunkKey.containing(0, Double.POSITIVE_INFINITY));
        assertEquals(Optional.empty(), ChunkKey.containing(31_000_000.0, 0));
        assertEquals(Optional.empty(), ChunkKey.containing(0, -1.0E300));
    }

    @Test
    void acceptsTheLastChunkOfTheWorld() {
        assertEquals(ChunkKey.MAX_COORDINATE, new ChunkKey(ChunkKey.MAX_COORDINATE, -ChunkKey.MAX_COORDINATE).x());
    }

    @Test
    void rejectsChunksOutsideTheWorld() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkKey(ChunkKey.MAX_COORDINATE + 1, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkKey(0, -ChunkKey.MAX_COORDINATE - 1));
        assertThrows(IllegalArgumentException.class, () -> new ChunkKey(Integer.MIN_VALUE, 0));
    }
}

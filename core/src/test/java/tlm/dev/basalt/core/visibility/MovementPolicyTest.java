package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovementPolicyTest {

    private static final Optional<ChunkKey> ORIGIN = Optional.of(new ChunkKey(0, 0));

    @Test
    void relaxesNothingInTheRealWorld() {
        assertFalse(MovementPolicy.relaxes(Optional.empty(), 8, 8));
    }

    @Test
    void relaxesAPlayerStandingInsideTheVirtualChunk() {
        assertTrue(MovementPolicy.relaxes(ORIGIN, 8.5, 8.5));
        assertTrue(MovementPolicy.relaxes(Optional.of(new ChunkKey(64, 64)), 1032.5, 1032.5));
    }

    @Test
    void relaxesAPlayerWhoseFeetStillTouchTheVirtualChunk() {
        assertTrue(MovementPolicy.relaxes(ORIGIN, 16.2, 8));
        assertTrue(MovementPolicy.relaxes(ORIGIN, -0.25, 8));
        assertTrue(MovementPolicy.relaxes(ORIGIN, 8, 16.29));
    }

    @Test
    void keepsTheRulesForAPlayerBesideTheVirtualChunk() {
        assertFalse(MovementPolicy.relaxes(ORIGIN, 16.4, 8));
        assertFalse(MovementPolicy.relaxes(ORIGIN, -0.4, 8));
        assertFalse(MovementPolicy.relaxes(ORIGIN, 8, -0.31));
        assertFalse(MovementPolicy.relaxes(ORIGIN, 40, 40));
    }
}

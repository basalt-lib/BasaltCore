package tlm.dev.basalt.core.interaction;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.Point;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InteractionValidatorTest {

    private static final BlockPosition BLOCK = new BlockPosition(1029, -58, 1029);
    private static final float BASALT_WITH_WOODEN_PICKAXE = 2.0F / 1.25F / 30;
    private static final float GOLD_WITH_IRON_PICKAXE = 6.0F / 3.0F / 30;
    private static final float BASALT_BY_HAND = 1.0F / 1.25F / 100;

    @Test
    void ignoresNegativeAndOldSequencesAndBadHitPoints() {
        assertEquals(Optional.of(Refusal.MALFORMED), screen(InteractionRequest.breaking(InteractionKind.START_BREAK, BLOCK, BlockFace.UP, -1), 0));
        assertEquals(Optional.of(Refusal.MALFORMED), screen(InteractionRequest.breaking(InteractionKind.STOP_BREAK, BLOCK, BlockFace.UP, 4), 4));
        assertEquals(Optional.of(Refusal.MALFORMED), screen(InteractionRequest.using(BLOCK, BlockFace.UP, new Point(Double.NaN, 0, 0), Hand.MAIN, 9), 4));
        assertEquals(Optional.of(Refusal.MALFORMED), screen(InteractionRequest.breaking(InteractionKind.ABORT_BREAK, BLOCK, BlockFace.UP, -3), 4));
        assertEquals(Optional.empty(), screen(InteractionRequest.breaking(InteractionKind.ABORT_BREAK, BLOCK, BlockFace.UP, 0), 4));
        assertEquals(Optional.empty(), screen(InteractionRequest.breaking(InteractionKind.START_BREAK, BLOCK, BlockFace.UP, 0), -1));
        assertEquals(Optional.empty(), screen(InteractionRequest.using(BLOCK, BlockFace.UP, new Point(1029.5, -57, 1029.5), Hand.OFF, 5), 4));
    }

    @Test
    void measuresReachFromTheEyesToTheBlockBox() {
        Point eyes = new Point(1029.5, -58.38, 1027.5);

        assertTrue(InteractionValidator.withinReach(eyes, BLOCK, 4.5, 1.0));
        assertTrue(InteractionValidator.withinReach(eyes, new BlockPosition(1029, -58, 1032), 4.5, 1.0));
        assertFalse(InteractionValidator.withinReach(eyes, new BlockPosition(1029, -58, 1034), 4.5, 1.0));
        assertFalse(InteractionValidator.withinReach(eyes, new BlockPosition(1029, -58, 1032), 4.5, 0.0));
        assertTrue(InteractionValidator.withinReach(new Point(1029.5, -57.5, 1029.5), BLOCK, 0.0, 0.1));
    }

    @Test
    void keepsTheHitPointNearTheBlock() {
        assertTrue(InteractionValidator.hitInsideBlock(new Point(1029.5, -57.0, 1029.5), BLOCK));
        assertTrue(InteractionValidator.hitInsideBlock(new Point(1030.5, -56.5, 1028.5), BLOCK));
        assertFalse(InteractionValidator.hitInsideBlock(new Point(1029.5, -55.0, 1029.5), BLOCK));
        assertFalse(InteractionValidator.hitInsideBlock(new Point(1027.4, -57.5, 1029.5), BLOCK));
    }

    @Test
    void stopsBreakingAtTheThresholdsOfTheRealServer() {
        assertFalse(InteractionValidator.breaksAtStop(BASALT_WITH_WOODEN_PICKAXE, 12));
        assertTrue(InteractionValidator.breaksAtStop(BASALT_WITH_WOODEN_PICKAXE, 13));
        assertFalse(InteractionValidator.breaksWhenDelayed(BASALT_WITH_WOODEN_PICKAXE, 17));
        assertTrue(InteractionValidator.breaksWhenDelayed(BASALT_WITH_WOODEN_PICKAXE, 18));
        assertFalse(InteractionValidator.breaksAtStop(GOLD_WITH_IRON_PICKAXE, 9));
        assertTrue(InteractionValidator.breaksAtStop(GOLD_WITH_IRON_PICKAXE, 10));
        assertFalse(InteractionValidator.breaksWhenDelayed(GOLD_WITH_IRON_PICKAXE, 13));
        assertTrue(InteractionValidator.breaksWhenDelayed(GOLD_WITH_IRON_PICKAXE, 14));
        assertFalse(InteractionValidator.breaksAtStop(BASALT_BY_HAND, 86));
        assertTrue(InteractionValidator.breaksAtStop(BASALT_BY_HAND, 87));
        assertFalse(InteractionValidator.breaksWhenDelayed(BASALT_BY_HAND, 123));
        assertTrue(InteractionValidator.breaksWhenDelayed(BASALT_BY_HAND, 124));
        assertTrue(InteractionValidator.breaksAtOnce(1.0F));
        assertFalse(InteractionValidator.breaksAtOnce(0.99F));
    }

    private static Optional<Refusal> screen(InteractionRequest request, int lastSequence) {
        return InteractionValidator.screen(request, lastSequence);
    }
}

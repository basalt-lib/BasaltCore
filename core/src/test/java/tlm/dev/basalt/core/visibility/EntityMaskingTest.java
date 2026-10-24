package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityMaskingTest {

    private static final UUID VIEWER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID STAND = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID ITEM = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID COW = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final ChunkRange ARENA = ChunkRange.of(new ChunkKey(64, 64));
    private static final ChunkRange NEIGHBOUR = ChunkRange.of(new ChunkKey(65, 64));
    private static final Point IN_ARENA = new Point(1032.5, -58, 1032.5);
    private static final Point IN_NEIGHBOUR = new Point(1044.5, -58, 1032.5);

    private final EntityMasking masking = new EntityMasking(100);

    @Test
    void hidesTheRealEntitiesThatEnterTheVirtualChunk() {
        EntityMask mask = masking.decide(VIEWER, Optional.of(ARENA),
                List.of(new RealEntity(STAND, IN_ARENA), new RealEntity(ITEM, IN_ARENA)), Set.of());

        assertEquals(Set.of(STAND, ITEM), Set.copyOf(mask.hide()));
        assertEquals(List.of(), mask.show());
    }

    @Test
    void leavesAlreadyHiddenEntitiesAlone() {
        EntityMask mask = masking.decide(VIEWER, Optional.of(ARENA), List.of(new RealEntity(STAND, IN_ARENA)), Set.of(STAND));

        assertTrue(mask.isEmpty());
    }

    @Test
    void showsAHiddenEntityThatLeftTheVirtualChunk() {
        EntityMask mask = masking.decide(VIEWER, Optional.of(ARENA),
                List.of(new RealEntity(STAND, IN_NEIGHBOUR), new RealEntity(ITEM, IN_ARENA)), Set.of(STAND, ITEM));

        assertEquals(List.of(), mask.hide());
        assertEquals(List.of(STAND), mask.show());
    }

    @Test
    void showsAHiddenEntityThatIsNoLongerNearby() {
        EntityMask mask = masking.decide(VIEWER, Optional.of(ARENA), List.of(), Set.of(COW));

        assertEquals(List.of(COW), mask.show());
    }

    @Test
    void showsEverythingWhenThePlayerLeavesItsVirtualChunk() {
        EntityMask mask = masking.decide(VIEWER, Optional.empty(), List.of(new RealEntity(STAND, IN_ARENA)), Set.of(STAND, ITEM));

        assertEquals(List.of(), mask.hide());
        assertEquals(Set.of(STAND, ITEM), Set.copyOf(mask.show()));
    }

    @Test
    void neverHidesTheViewerItself() {
        EntityMask mask = masking.decide(VIEWER, Optional.of(ARENA), List.of(new RealEntity(VIEWER, IN_ARENA)), Set.of());

        assertTrue(mask.isEmpty());
        assertFalse(EntityMasking.hides(VIEWER, Optional.of(ARENA), new RealEntity(VIEWER, IN_ARENA)));
    }

    @Test
    void leavesThePlayersToTheSeparationRule() {
        RealEntity player = new RealEntity(STAND, IN_ARENA, true);

        assertFalse(EntityMasking.hides(VIEWER, Optional.of(ARENA), player));
        assertTrue(masking.decide(VIEWER, Optional.of(ARENA), List.of(player), Set.of()).isEmpty());
    }

    @Test
    void hidesOnlyWhatStandsInTheVirtualChunk() {
        EntityMask mask = masking.decide(VIEWER, Optional.of(ARENA),
                List.of(new RealEntity(STAND, IN_ARENA), new RealEntity(COW, IN_NEIGHBOUR)), Set.of());

        assertEquals(List.of(STAND), mask.hide());
        assertTrue(EntityMasking.hides(VIEWER, Optional.of(ARENA), new RealEntity(STAND, IN_ARENA)));
        assertFalse(EntityMasking.hides(VIEWER, Optional.of(ARENA), new RealEntity(COW, IN_NEIGHBOUR)));
        assertFalse(EntityMasking.hides(VIEWER, Optional.empty(), new RealEntity(STAND, IN_ARENA)));
    }

    @Test
    void movesTheMaskWhenThePlayerTakesAnotherChunk() {
        List<RealEntity> nearby = List.of(new RealEntity(STAND, IN_ARENA), new RealEntity(COW, IN_NEIGHBOUR));

        EntityMask mask = masking.decide(VIEWER, Optional.of(NEIGHBOUR), nearby, Set.of(STAND));

        assertEquals(List.of(COW), mask.hide());
        assertEquals(List.of(STAND), mask.show());
    }

    @Test
    void changesAtMostItsLimitPerScan() {
        EntityMasking small = new EntityMasking(2);
        List<RealEntity> crowd = List.of(new RealEntity(STAND, IN_ARENA), new RealEntity(ITEM, IN_ARENA), new RealEntity(COW, IN_ARENA));

        EntityMask first = small.decide(VIEWER, Optional.of(ARENA), crowd, Set.of());
        EntityMask second = small.decide(VIEWER, Optional.of(ARENA), crowd, Set.copyOf(first.hide()));

        assertEquals(List.of(STAND, ITEM), first.hide());
        assertEquals(List.of(COW), second.hide());
        assertThrows(IllegalArgumentException.class, () -> new EntityMasking(0));
    }

    @Test
    void anEntityOutsideTheWorldIsNowhere() {
        RealEntity lost = new RealEntity(COW, new Point(Double.NaN, 0, 0));

        assertFalse(lost.isIn(ARENA));
        assertTrue(masking.decide(VIEWER, Optional.of(ARENA), List.of(lost), Set.of()).isEmpty());
    }

    @Test
    void boundsTheScanSettings() {
        assertEquals(5, new EntityMaskSettings(5, 128).scanIntervalTicks());
        assertThrows(IllegalArgumentException.class, () -> new EntityMaskSettings(0, 128));
        assertThrows(IllegalArgumentException.class, () -> new EntityMaskSettings(201, 128));
        assertThrows(IllegalArgumentException.class, () -> new EntityMaskSettings(5, 0));
        assertThrows(IllegalArgumentException.class, () -> new EntityMaskSettings(5, 10_001));
    }
}

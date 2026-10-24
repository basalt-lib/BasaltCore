package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewSnapshotTest {

    private static final UUID WORLD = new UUID(0, 1);

    @Test
    void hidesOnlyTheChunksOfItsArea() {
        ViewSnapshot snapshot = ViewSnapshot.of(WORLD, new ChunkRange(2, 3, 3, 3));

        assertTrue(snapshot.isVirtual());
        assertTrue(snapshot.hides(new ChunkKey(2, 3)));
        assertTrue(snapshot.hides(new ChunkKey(3, 3)));
        assertFalse(snapshot.hides(new ChunkKey(3, 2)));
        assertFalse(snapshot.hides(null));
    }

    @Test
    void hidesNothingInTheRealWorld() {
        ViewSnapshot snapshot = ViewSnapshot.real();

        assertFalse(snapshot.isVirtual());
        assertFalse(snapshot.hides(new ChunkKey(0, 0)));
    }

    @Test
    void needsAWorldAndAnAreaTogether() {
        assertThrows(IllegalArgumentException.class, () -> ViewSnapshot.of(WORLD, null));
        assertThrows(IllegalArgumentException.class, () -> ViewSnapshot.of(null, ChunkRange.of(new ChunkKey(0, 0))));
        assertThrows(IllegalArgumentException.class, () -> new ViewSnapshot(null, ChunkRange.of(new ChunkKey(0, 0)), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ViewSnapshot(null, null, Set.of(7)));
    }

    @Test
    void carriesTheIdsOfItsVirtualEntitiesWithoutSharingThem() {
        Set<Integer> ids = new HashSet<>(Set.of(1001, 1002));
        ViewSnapshot snapshot = ViewSnapshot.of(WORLD, ChunkRange.of(new ChunkKey(2, 3))).withEntities(ids);
        ids.add(1003);

        assertTrue(snapshot.holdsEntity(1001));
        assertFalse(snapshot.holdsEntity(1003));
        assertFalse(snapshot.holdsEntity(-1));
        assertFalse(ViewSnapshot.real().holdsEntity(1001));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.virtualEntities().add(9));
    }

    @Test
    void factsNeedAChunkUnlessUnrelated() {
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(PacketKind.BLOCK, null));
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(PacketKind.OTHER, new ChunkKey(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(null, new ChunkKey(0, 0)));
    }
}

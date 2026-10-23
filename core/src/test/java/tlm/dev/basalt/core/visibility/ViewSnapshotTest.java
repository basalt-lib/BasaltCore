package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ViewSnapshotTest {

    @Test
    void hidesOnlyItsVirtualChunk() {
        ViewSnapshot<String> snapshot = ViewSnapshot.virtual(new ChunkKey(2, 3), "payload");

        assertTrue(snapshot.isVirtual());
        assertTrue(snapshot.hides(new ChunkKey(2, 3)));
        assertFalse(snapshot.hides(new ChunkKey(3, 2)));
        assertFalse(snapshot.hides(null));
    }

    @Test
    void hidesNothingInTheRealWorld() {
        ViewSnapshot<String> snapshot = ViewSnapshot.real();

        assertFalse(snapshot.isVirtual());
        assertFalse(snapshot.hides(new ChunkKey(0, 0)));
    }

    @Test
    void needsAChunkAndAPayloadTogether() {
        assertThrows(IllegalArgumentException.class, () -> ViewSnapshot.virtual(new ChunkKey(0, 0), null));
        assertThrows(IllegalArgumentException.class, () -> ViewSnapshot.virtual(null, "payload"));
        assertThrows(IllegalArgumentException.class, () -> new ViewSnapshot<>(null, "payload", Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new ViewSnapshot<>(null, null, Set.of(7)));
    }

    @Test
    void carriesTheIdsOfItsVirtualEntitiesWithoutSharingThem() {
        Set<Integer> ids = new HashSet<>(Set.of(1001, 1002));
        ViewSnapshot<String> snapshot = ViewSnapshot.virtual(new ChunkKey(2, 3), "payload").withEntities(ids);
        ids.add(1003);

        assertTrue(snapshot.holdsEntity(1001));
        assertFalse(snapshot.holdsEntity(1003));
        assertFalse(snapshot.holdsEntity(-1));
        assertTrue(snapshot.withPayload("changed").holdsEntity(1002));
        assertFalse(ViewSnapshot.<String>real().holdsEntity(1001));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.virtualEntities().add(9));
    }

    @Test
    void factsNeedAChunkUnlessUnrelated() {
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(PacketKind.BLOCK, null));
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(PacketKind.OTHER, new ChunkKey(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(null, new ChunkKey(0, 0)));
    }
}

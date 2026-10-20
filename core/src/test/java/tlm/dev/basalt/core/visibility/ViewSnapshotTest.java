package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

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
        assertThrows(IllegalArgumentException.class, () -> new ViewSnapshot<>(null, "payload"));
    }

    @Test
    void factsNeedAChunkUnlessUnrelated() {
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(PacketKind.BLOCK, null));
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(PacketKind.OTHER, new ChunkKey(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> PacketFacts.about(null, new ChunkKey(0, 0)));
    }
}

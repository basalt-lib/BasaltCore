package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerViewTest {

    private final PlayerView<String> view = new PlayerView<>();

    @Test
    void startsInTheRealWorld() {
        assertFalse(view.snapshot().isVirtual());
    }

    @Test
    void publishesTheVirtualChunkWithItsPayload() {
        assertTrue(view.enter(new ChunkKey(1, 2), "payload"));

        assertTrue(view.snapshot().hides(new ChunkKey(1, 2)));
        assertEquals("payload", view.snapshot().virtualPayload());
    }

    @Test
    void keepsTheFirstVirtualChunk() {
        view.enter(new ChunkKey(1, 2), "first");

        assertFalse(view.enter(new ChunkKey(3, 4), "second"));
        assertEquals("first", view.snapshot().virtualPayload());
    }

    @Test
    void returnsTheChunkItLeaves() {
        view.enter(new ChunkKey(1, 2), "payload");

        assertEquals(Optional.of(new ChunkKey(1, 2)), view.exit());
        assertFalse(view.snapshot().isVirtual());
        assertEquals(Optional.empty(), view.exit());
    }

    @Test
    void neverChangesAPublishedSnapshot() {
        view.enter(new ChunkKey(1, 2), "payload");
        ViewSnapshot<String> published = view.snapshot();

        view.exit();

        assertTrue(published.hides(new ChunkKey(1, 2)));
    }

    @Test
    void keepsOneViewPerPlayer() {
        PlayerViews<String> views = new PlayerViews<>();
        UUID player = UUID.randomUUID();

        PlayerView<String> opened = views.open(player);

        assertSame(opened, views.open(player));
        assertEquals(Optional.of(opened), views.find(player));
        views.close(player);
        assertEquals(Optional.empty(), views.find(player));
    }
}

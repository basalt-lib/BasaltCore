package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerViewTest {

    private static final UUID WORLD = new UUID(0, 1);
    private static final UUID OTHER_WORLD = new UUID(0, 2);
    private static final ChunkRange AREA = new ChunkRange(1, 2, 2, 2);

    private final PlayerView view = new PlayerView();

    @Test
    void startsInTheRealWorld() {
        assertFalse(view.snapshot().isVirtual());
    }

    @Test
    void publishesTheWorldWithItsArea() {
        assertTrue(view.enter(WORLD, AREA));

        assertTrue(view.snapshot().hides(new ChunkKey(1, 2)));
        assertTrue(view.snapshot().hides(new ChunkKey(2, 2)));
        assertEquals(WORLD, view.snapshot().world());
    }

    @Test
    void publishesItsEntitiesUntilItLeaves() {
        assertFalse(view.publishEntities(Set.of(1001)));

        view.enter(WORLD, AREA);
        assertTrue(view.publishEntities(Set.of(1001)));

        assertTrue(view.snapshot().holdsEntity(1001));
        view.exit();
        assertFalse(view.snapshot().holdsEntity(1001));
        assertFalse(view.publishEntities(Set.of(1002)));
    }

    @Test
    void keepsTheFirstWorld() {
        view.enter(WORLD, AREA);

        assertFalse(view.enter(OTHER_WORLD, ChunkRange.of(new ChunkKey(3, 4))));
        assertEquals(WORLD, view.snapshot().world());
    }

    @Test
    void returnsTheViewItLeaves() {
        view.enter(WORLD, AREA);

        assertEquals(Optional.of(ViewSnapshot.of(WORLD, AREA)), view.exit());
        assertFalse(view.snapshot().isVirtual());
        assertEquals(Optional.empty(), view.exit());
    }

    @Test
    void neverChangesAPublishedSnapshot() {
        view.enter(WORLD, AREA);
        ViewSnapshot published = view.snapshot();

        view.exit();

        assertTrue(published.hides(new ChunkKey(1, 2)));
    }

    @Test
    void keepsOneViewPerPlayer() {
        PlayerViews views = new PlayerViews();
        UUID player = UUID.randomUUID();

        PlayerView opened = views.open(player);

        assertSame(opened, views.open(player));
        assertEquals(Optional.of(opened), views.find(player));
        views.close(player);
        assertEquals(Optional.empty(), views.find(player));
    }
}

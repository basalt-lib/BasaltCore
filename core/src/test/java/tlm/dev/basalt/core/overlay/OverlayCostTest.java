package tlm.dev.basalt.core.overlay;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlayCostTest {

    private static final int PLAYERS = 200;
    private static final int BLOCKS = 50;
    private static final int ENTITIES = 50;
    private static final UUID WORLD = new UUID(0, 1);
    private static final ChunkRange AREA = new ChunkRange(0, 0, 15, 15);

    @Test
    void twoHundredPlayersWithFiftyBlocksAndFiftyEntitiesEachStayWithinTheShippedLimits() {
        CountingDisplay display = new CountingDisplay();
        OverlayViews<String> views = new OverlayViews<>();
        AtomicInteger ids = new AtomicInteger();
        Overlays<String> overlays = new Overlays<>(new OverlayTools<>(display, views, () -> new UUID(7, ids.incrementAndGet()),
                () -> 1_000_000 + ids.incrementAndGet()), new OverlaySettings(1024, 4096, 256));
        List<UUID> players = new ArrayList<>();
        for (int player = 0; player < PLAYERS; player++) {
            UUID viewer = new UUID(1, player);
            UUID overlay = overlays.create("Consumer", WORLD, AREA, -64, 319).orElseThrow();
            for (int spot = 0; spot < BLOCKS; spot++) {
                assertEquals(OverlayEdit.CHANGED, overlays.setBlock(overlay, new BlockPosition(spot * 5, 64, spot * 3), 3000 + spot % 3));
            }
            for (int spot = 0; spot < ENTITIES; spot++) {
                assertEquals(OverlayEdit.CHANGED, overlays.spawn(overlay, "minecraft:item", new Point(spot * 5 + 2.5, 65, spot * 3 + 0.5),
                        0, 0, "stack").result());
            }
            assertEquals(OverlayJoin.ADDED, overlays.addViewer(overlay, viewer));
            players.add(viewer);
        }

        assertEquals(PLAYERS * BLOCKS, display.blocksShown);
        assertEquals(PLAYERS * ENTITIES, display.entitiesSpawned);
        assertEquals(PLAYERS, overlays.open().size());
        for (UUID player : players) {
            OverlaySnapshot<String> seen = views.current(player);
            assertEquals(BLOCKS, seen.blocks().size());
            assertEquals(ENTITIES, seen.entities().size());
            assertTrue(seen.claimsBlock(new BlockPosition(0, 64, 0)));
            assertEquals(OverlayBlockIndex.NONE, seen.stateAt(1, 64, 0));
        }

        assertEquals(PLAYERS, overlays.closeAll(OverlayExit.SHUTDOWN));
        assertEquals(PLAYERS * BLOCKS, display.realShown);
        assertEquals(PLAYERS * ENTITIES, display.entitiesRemoved);
    }

    private static final class CountingDisplay implements OverlayDisplay<String> {

        private int blocksShown;
        private int realShown;
        private int entitiesSpawned;
        private int entitiesRemoved;

        @Override
        public boolean holdsChunk(UUID player, ChunkKey chunk) {
            return true;
        }

        @Override
        public void show(UUID player, List<OverlayBlock> blocks) {
            blocksShown += blocks.size();
        }

        @Override
        public void showReal(UUID player, UUID world, List<BlockPosition> positions) {
            realShown += positions.size();
        }

        @Override
        public void blockAction(UUID player, OverlayBlock block, int action, int parameter) {
        }

        @Override
        public void spawn(UUID player, OverlayEntity<String> entity) {
            entitiesSpawned++;
        }

        @Override
        public void move(UUID player, OverlayEntity<String> entity) {
        }

        @Override
        public void update(UUID player, OverlayEntity<String> entity) {
        }

        @Override
        public void remove(UUID player, List<Integer> entities) {
            entitiesRemoved += entities.size();
        }
    }
}

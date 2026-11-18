package tlm.dev.basalt.core.overlay;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.BlockFace;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OverlaysTest {

    private static final UUID WORLD = new UUID(0, 1);
    private static final UUID ALICE = new UUID(1, 1);
    private static final UUID BOB = new UUID(1, 2);
    private static final ChunkRange AREA = new ChunkRange(64, 64, 65, 65);
    private static final int CHEST = 3000;
    private static final int BARREL = 2000;
    private static final BlockPosition SPOT = new BlockPosition(1030, -60, 1030);

    private final RecordingDisplay display = new RecordingDisplay();
    private final OverlayViews<String> views = new OverlayViews<>();
    private final AtomicInteger ids = new AtomicInteger();
    private final Overlays<String> overlays = new Overlays<>(new OverlayTools<>(display, views,
            () -> new UUID(9, ids.incrementAndGet()), () -> 500 + ids.incrementAndGet()), new OverlaySettings(3, 4, 2));

    @Test
    void showsItsBlocksToTheViewersThatHoldTheChunk() {
        UUID overlay = open();
        display.held.add(new ChunkKey(64, 64));

        assertEquals(OverlayEdit.CHANGED, overlays.setBlock(overlay, SPOT, CHEST));
        assertEquals(List.of(), display.calls);
        assertEquals(OverlayJoin.ADDED, overlays.addViewer(overlay, ALICE));
        assertEquals(OverlayEdit.CHANGED, overlays.setBlock(overlay, new BlockPosition(1050, -60, 1030), BARREL));

        assertEquals(List.of("show alice 1030 -60 1030=3000"), display.calls);
        assertEquals(CHEST, views.current(ALICE).stateAt(1030, -60, 1030));
        assertEquals(BARREL, views.current(ALICE).stateAt(1050, -60, 1030));
        assertTrue(views.current(BOB).isNone());
    }

    @Test
    void leavesAnotherOverlayOfTheSameAreaUntouched() {
        UUID first = open();
        UUID second = open();
        display.held.add(new ChunkKey(64, 64));
        overlays.addViewer(first, ALICE);
        overlays.addViewer(second, BOB);

        overlays.setBlock(first, SPOT, CHEST);
        overlays.setBlock(second, SPOT, BARREL);

        assertEquals(List.of("show alice 1030 -60 1030=3000", "show bob 1030 -60 1030=2000"), display.calls);
        assertEquals(CHEST, views.current(ALICE).stateAt(1030, -60, 1030));
        assertEquals(BARREL, views.current(BOB).stateAt(1030, -60, 1030));
        assertEquals(OptionalInt.of(CHEST), overlays.blockSeenBy(ALICE, SPOT));
        assertEquals(OptionalInt.of(BARREL), overlays.blockSeenBy(BOB, SPOT));
    }

    @Test
    void givesTheRealBlockBackWhenABlockIsRemoved() {
        UUID overlay = open();
        display.held.add(new ChunkKey(64, 64));
        overlays.addViewer(overlay, ALICE);
        overlays.setBlock(overlay, SPOT, CHEST);
        display.calls.clear();

        assertEquals(OverlayEdit.CHANGED, overlays.removeBlockSeenBy(ALICE, SPOT));
        assertEquals(OverlayEdit.UNCHANGED, overlays.removeBlock(overlay, SPOT));

        assertEquals(List.of("real alice 1030 -60 1030"), display.calls);
        assertEquals(OverlayBlockIndex.NONE, views.current(ALICE).stateAt(1030, -60, 1030));
    }

    @Test
    void sendsEverythingOnEntryAndTheRealWorldOnExit() {
        UUID overlay = open();
        display.held.add(new ChunkKey(64, 64));
        overlays.setBlock(overlay, SPOT, CHEST);
        overlays.setBlock(overlay, new BlockPosition(1050, -60, 1030), BARREL);
        OverlayEntity<String> item = overlays.spawn(overlay, "minecraft:item", new Point(1031.5, -59.5, 1030.5), 0, 0, "diamond")
                .entity().orElseThrow();

        overlays.addViewer(overlay, ALICE);
        overlays.removeViewer(ALICE, OverlayExit.REQUESTED);

        assertEquals(List.of("show alice 1030 -60 1030=3000", "spawn alice " + item.id() + " diamond",
                "real alice 1030 -60 1030", "remove alice [" + item.id() + "]"), display.calls);
        assertTrue(views.current(ALICE).isNone());
        assertEquals(Optional.empty(), overlays.overlayOf(ALICE));
    }

    @Test
    void forgetsADisconnectedViewerWithoutPackets() {
        UUID overlay = open();
        display.held.add(new ChunkKey(64, 64));
        overlays.setBlock(overlay, SPOT, CHEST);
        overlays.addViewer(overlay, ALICE);
        display.calls.clear();

        overlays.forget(ALICE);
        overlays.removeViewer(BOB, OverlayExit.REAL_WORLD_CHANGE);

        assertEquals(List.of(), display.calls);
        assertEquals(Set.of(), overlays.viewers(overlay));
        assertEquals(0, views.size());
        assertTrue(overlays.isOpen(overlay));
    }

    @Test
    void closingRestoresEveryViewerAndRefusesEverythingAfter() {
        UUID overlay = open();
        display.held.add(new ChunkKey(64, 64));
        overlays.setBlock(overlay, SPOT, CHEST);
        overlays.addViewer(overlay, ALICE);
        overlays.addViewer(overlay, BOB);
        display.calls.clear();

        assertTrue(overlays.close(overlay, OverlayExit.CLOSED));
        assertFalse(overlays.close(overlay, OverlayExit.CLOSED));

        assertEquals(List.of("real alice 1030 -60 1030", "real bob 1030 -60 1030"), display.calls);
        assertEquals(OverlayEdit.CLOSED, overlays.setBlock(overlay, SPOT, CHEST));
        assertEquals(OverlayJoin.CLOSED, overlays.addViewer(overlay, ALICE));
        assertEquals(OverlayEdit.CLOSED, overlays.spawn(overlay, "minecraft:item", new Point(1030, -60, 1030), 0, 0, "x").result());
        assertTrue(views.current(ALICE).isNone());
        assertFalse(overlays.isOpen(overlay));
    }

    @Test
    void holdsItsLimitsAndItsArea() {
        UUID overlay = open();
        open();
        open();

        assertEquals(Optional.empty(), overlays.create("Owner", WORLD, AREA, -64, 319));
        assertEquals(OverlayEdit.OUTSIDE, overlays.setBlock(overlay, new BlockPosition(1056, -60, 1030), CHEST));
        assertEquals(OverlayEdit.OUTSIDE, overlays.setBlock(overlay, new BlockPosition(1030, 320, 1030), CHEST));
        for (int x = 0; x < 4; x++) {
            assertEquals(OverlayEdit.CHANGED, overlays.setBlock(overlay, new BlockPosition(1024 + x, -60, 1024), CHEST));
        }
        assertEquals(OverlayEdit.TOO_MANY, overlays.setBlock(overlay, new BlockPosition(1030, -60, 1024), CHEST));
        assertEquals(OverlayEdit.UNCHANGED, overlays.setBlock(overlay, new BlockPosition(1024, -60, 1024), CHEST));
        assertEquals(OverlayEdit.CHANGED, overlays.setBlock(overlay, new BlockPosition(1024, -60, 1024), BARREL));
        overlays.spawn(overlay, "minecraft:item", new Point(1030, -60, 1030), 0, 0, "a");
        overlays.spawn(overlay, "minecraft:item", new Point(1030, -60, 1030), 0, 0, "b");
        assertEquals(OverlayEdit.TOO_MANY, overlays.spawn(overlay, "minecraft:item", new Point(1030, -60, 1030), 0, 0, "c").result());
        assertEquals(OverlayEdit.OUTSIDE, overlays.spawn(overlay, "minecraft:item", new Point(1000, -60, 1030), 0, 0, "c").result());
        assertThrows(IllegalArgumentException.class, () -> overlays.setBlock(overlay, SPOT, -1));
    }

    @Test
    void aPlayerViewsOneOverlayAtATime() {
        UUID first = open();
        UUID second = open();

        assertEquals(OverlayJoin.ADDED, overlays.addViewer(first, ALICE));
        assertEquals(OverlayJoin.ALREADY_VIEWING, overlays.addViewer(first, ALICE));
        assertEquals(OverlayJoin.ALREADY_VIEWING, overlays.addViewer(second, ALICE));
        assertEquals(Optional.of(first), overlays.overlayOf(ALICE));
    }

    @Test
    void movesAnEntityAcrossChunksForEachViewer() {
        UUID overlay = open();
        display.held.add(new ChunkKey(64, 64));
        overlays.addViewer(overlay, ALICE);
        OverlayEntity<String> item = overlays.spawn(overlay, "minecraft:item", new Point(1030, -60, 1030), 0, 0, "gem")
                .entity().orElseThrow();
        display.calls.clear();

        overlays.moveEntity(overlay, item.id(), new Point(1031, -60, 1030), 0, 0);
        overlays.moveEntity(overlay, item.id(), new Point(1050, -60, 1030), 0, 0);
        display.held.add(new ChunkKey(65, 64));
        overlays.moveEntity(overlay, item.id(), new Point(1051, -60, 1030), 0, 0);
        overlays.changeLook(overlay, item.id(), "emerald");
        overlays.removeEntity(overlay, item.id());

        assertEquals(List.of("move alice " + item.id(), "remove alice [" + item.id() + "]", "move alice " + item.id(),
                "update alice " + item.id() + " emerald", "remove alice [" + item.id() + "]"), display.calls);
        assertEquals(OverlayEdit.NOT_FOUND, overlays.removeEntity(overlay, item.id()));
        assertFalse(views.current(ALICE).holdsEntity(item.id()));
    }

    @Test
    void claimsTheClicksOnItsBlocksAndItsEntitiesOnly() {
        UUID overlay = open();
        overlays.setBlock(overlay, SPOT, CHEST);
        OverlayEntity<String> item = overlays.spawn(overlay, "minecraft:interaction", new Point(1031, -60, 1030), 0, 0, "box")
                .entity().orElseThrow();
        overlays.addViewer(overlay, ALICE);
        OverlaySnapshot<String> seen = views.current(ALICE);

        assertTrue(seen.claimsBlock(SPOT));
        assertFalse(seen.claimsBlock(new BlockPosition(1030, -61, 1030)));
        assertTrue(seen.claimsUse(new BlockPosition(1030, -61, 1030), BlockFace.UP));
        assertFalse(seen.claimsUse(new BlockPosition(1030, -61, 1030), BlockFace.DOWN));
        assertTrue(seen.holdsEntity(item.id()));
        assertFalse(seen.holdsEntity(item.id() + 1));
        assertTrue(seen.touches(64, 64));
        assertFalse(seen.touches(65, 65));
        assertFalse(seen.touches(70, 70));
        assertEquals(List.of(item), seen.entitiesIn(64, 64));
    }

    @Test
    void playsABlockActionOnlyOnAnOverlayBlock() {
        UUID overlay = open();
        display.held.add(new ChunkKey(64, 64));
        overlays.addViewer(overlay, ALICE);
        overlays.setBlock(overlay, SPOT, CHEST);
        display.calls.clear();

        assertEquals(OverlayEdit.CHANGED, overlays.blockAction(overlay, SPOT, 1, 1));
        assertEquals(OverlayEdit.NOT_FOUND, overlays.blockAction(overlay, new BlockPosition(1031, -60, 1030), 1, 1));

        assertEquals(List.of("action alice 1030 -60 1030=3000 1 1"), display.calls);
    }

    @Test
    void closesTheOverlaysOfOneOwner() {
        UUID mine = open();
        UUID theirs = overlays.create("Other", WORLD, AREA, -64, 319).orElseThrow();

        assertEquals(1, overlays.closeOwnedBy("Owner", OverlayExit.CLOSED));
        assertFalse(overlays.isOpen(mine));
        assertTrue(overlays.isOpen(theirs));
        assertEquals(1, overlays.closeAll(OverlayExit.SHUTDOWN));
        assertEquals(List.of(), overlays.open());
    }

    private UUID open() {
        return overlays.create("Owner", WORLD, AREA, -64, 319).orElseThrow();
    }

    private static final class RecordingDisplay implements OverlayDisplay<String> {

        private final Set<ChunkKey> held = new HashSet<>();
        private final List<String> calls = new ArrayList<>();

        @Override
        public boolean holdsChunk(UUID player, ChunkKey chunk) {
            return held.contains(chunk);
        }

        @Override
        public void show(UUID player, List<OverlayBlock> blocks) {
            for (OverlayBlock block : blocks) {
                calls.add("show " + name(player) + " " + text(block.position()) + "=" + block.state());
            }
        }

        @Override
        public void showReal(UUID player, UUID world, List<BlockPosition> positions) {
            for (BlockPosition position : positions) {
                calls.add("real " + name(player) + " " + text(position));
            }
        }

        @Override
        public void blockAction(UUID player, OverlayBlock block, int action, int parameter) {
            calls.add("action " + name(player) + " " + text(block.position()) + "=" + block.state() + " " + action + " " + parameter);
        }

        @Override
        public void spawn(UUID player, OverlayEntity<String> entity) {
            calls.add("spawn " + name(player) + " " + entity.id() + " " + entity.look());
        }

        @Override
        public void move(UUID player, OverlayEntity<String> entity) {
            calls.add("move " + name(player) + " " + entity.id());
        }

        @Override
        public void update(UUID player, OverlayEntity<String> entity) {
            calls.add("update " + name(player) + " " + entity.id() + " " + entity.look());
        }

        @Override
        public void remove(UUID player, List<Integer> entities) {
            calls.add("remove " + name(player) + " " + entities);
        }

        private static String name(UUID player) {
            return player.equals(ALICE) ? "alice" : "bob";
        }

        private static String text(BlockPosition position) {
            return position.x() + " " + position.y() + " " + position.z();
        }
    }
}

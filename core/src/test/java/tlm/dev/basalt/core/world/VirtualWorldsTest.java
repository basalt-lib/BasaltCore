package tlm.dev.basalt.core.world;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualWorldsTest {

    private static final UUID REAL = UUID.fromString("00000000-0000-0000-0000-0000000000f0");
    private static final UUID A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID C = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final HostArea TWO_BY_TWO = new HostArea(REAL, new ChunkRange(68, 64, 69, 65), -64, 384);
    private static final int STONE = 1;
    private static final int GOLD = 2;

    private final FakeWorldViewers viewers = new FakeWorldViewers();
    private final Map<ChunkKey, RealBlock> realBlocks = new HashMap<>();
    private final List<ChunkKey> realChunksRead = new ArrayList<>();
    private final RealChunks real = (world, chunk) -> {
        realChunksRead.add(chunk);
        return Optional.ofNullable(realBlocks.get(chunk));
    };
    private int nextId;
    private final VirtualWorlds<FakeChunk> worlds = new VirtualWorlds<>(new WorldTools<>(FakeChunk.CHUNKS, real, viewers, viewers),
            new WorldSettings(4, 2), () -> new UUID(0, ++nextId));

    @BeforeEach
    void exitThroughTheService() {
        viewers.exitPath = player -> worlds.worldOf(player).ifPresent(world -> worlds.viewerLeft(world, player));
    }

    @Test
    void startsEmptyWithoutMaterialisingAnyChunk() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));

        WorldSnapshot<FakeChunk> snapshot = worlds.snapshot(world).orElseThrow();
        assertEquals(Map.of(), snapshot.chunks());
        assertEquals(Set.of(), snapshot.viewers());
        assertEquals(OptionalInt.of(FakeChunk.AIR), worlds.blockAt(world, new BlockPosition(1100, -60, 1040)));
        assertEquals(List.of(world), worlds.open());
    }

    @Test
    void checksEveryRealChunkOfTheAreaForAir() {
        created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));

        assertEquals(TWO_BY_TWO.chunks().chunks(), realChunksRead);
    }

    @Test
    void refusesAnAreaHoldingARealBlockAndNamesTheFirstOne() {
        RealBlock grass = new RealBlock(new BlockPosition(1104, -61, 1040), "minecraft:grass_block");
        realBlocks.put(new ChunkKey(69, 65), grass);
        realBlocks.put(new ChunkKey(69, 64), new RealBlock(new BlockPosition(1104, -61, 1024), "minecraft:grass_block"));

        Creation creation = worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY);

        assertEquals(new Creation.Refused(CreationRefusal.REAL_BLOCKS,
                Optional.of(new RealBlock(new BlockPosition(1104, -61, 1024), "minecraft:grass_block"))), creation);
        assertEquals(List.of(), worlds.open());
    }

    @Test
    void canStandOverRealBlocksWhenAskedTo() {
        realBlocks.put(new ChunkKey(68, 64), new RealBlock(new BlockPosition(1088, -61, 1024), "minecraft:grass_block"));

        created(worlds.create(TWO_BY_TWO, HostRule.ANY_BLOCKS));

        assertEquals(List.of(), realChunksRead);
    }

    @Test
    void refusesMoreChunksOrWorldsThanTheSettingsAllow() {
        HostArea tooWide = new HostArea(REAL, new ChunkRange(0, 0, 4, 0), -64, 384);
        assertEquals(new Creation.Refused(CreationRefusal.TOO_MANY_CHUNKS, Optional.empty()), worlds.create(tooWide, HostRule.AIR_ONLY));
        assertEquals(List.of(), realChunksRead);

        created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        assertEquals(new Creation.Refused(CreationRefusal.TOO_MANY_WORLDS, Optional.empty()), worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
    }

    @Test
    void materialisesOnlyTheChunksItChangesAndSharesTheOthers() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        WorldSnapshot<FakeChunk> before = worlds.snapshot(world).orElseThrow();

        assertEquals(EditResult.CHANGED, worlds.setBlock(world, new BlockPosition(1090, -54, 1030), STONE));
        assertEquals(EditResult.CHANGED, worlds.setBlock(world, new BlockPosition(1110, -54, 1050), GOLD));
        WorldSnapshot<FakeChunk> middle = worlds.snapshot(world).orElseThrow();
        assertEquals(EditResult.CHANGED, worlds.setBlock(world, new BlockPosition(1091, -54, 1030), STONE));
        WorldSnapshot<FakeChunk> after = worlds.snapshot(world).orElseThrow();

        assertEquals(Map.of(), before.chunks());
        assertEquals(Set.of(new ChunkKey(68, 64), new ChunkKey(69, 65)), after.chunks().keySet());
        assertSame(middle.chunks().get(new ChunkKey(69, 65)), after.chunks().get(new ChunkKey(69, 65)));
        assertNotSame(middle.chunks().get(new ChunkKey(68, 64)), after.chunks().get(new ChunkKey(68, 64)));
        assertEquals(OptionalInt.of(GOLD), worlds.blockAt(world, new BlockPosition(1110, -54, 1050)));
        assertEquals(OptionalInt.of(FakeChunk.AIR), worlds.blockAt(world, new BlockPosition(1110, -54, 1030)));
    }

    @Test
    void keepsAPublishedSnapshotUnchangeable() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.setBlock(world, new BlockPosition(1090, -54, 1030), STONE);
        WorldSnapshot<FakeChunk> snapshot = worlds.snapshot(world).orElseThrow();

        worlds.setBlock(world, new BlockPosition(1090, -54, 1030), GOLD);

        assertEquals(STONE, snapshot.chunks().get(new ChunkKey(68, 64)).blocks().get(new BlockPosition(1090, -54, 1030)));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.chunks().clear());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.viewers().add(C));
    }

    @Test
    void refusesABlockOutsideTheAreaOrTheHeight() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));

        assertEquals(EditResult.OUTSIDE, worlds.setBlock(world, new BlockPosition(1120, -54, 1030), STONE));
        assertEquals(EditResult.OUTSIDE, worlds.setBlock(world, new BlockPosition(1090, 320, 1030), STONE));
        assertEquals(EditResult.OUTSIDE, worlds.setBlock(world, new BlockPosition(1090, -65, 1030), STONE));
        assertEquals(EditResult.OUTSIDE, worlds.fill(world, BlockBox.between(new BlockPosition(1088, -60, 1024), new BlockPosition(1120, -60, 1024)), STONE));
        assertEquals(OptionalInt.empty(), worlds.blockAt(world, new BlockPosition(1087, -54, 1030)));
        assertEquals(Map.of(), worlds.snapshot(world).orElseThrow().chunks());
    }

    @Test
    void leavesAnUntouchedChunkUnmaterialised() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));

        assertEquals(EditResult.UNCHANGED, worlds.setBlock(world, new BlockPosition(1090, -54, 1030), FakeChunk.AIR));

        assertEquals(Map.of(), worlds.snapshot(world).orElseThrow().chunks());
    }

    @Test
    void showsABlockSetThroughTheWorldToEveryViewer() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.addViewer(world, A);
        worlds.addViewer(world, B);
        viewers.events.clear();

        worlds.setBlock(world, new BlockPosition(1090, -54, 1030), STONE);

        assertEquals(List.of("block 1090 -54 1030 = 1 to [A, B]"), viewers.events);
    }

    @Test
    void showsABlockChangedByAViewerToTheOtherViewersOnly() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.addViewer(world, A);
        worlds.addViewer(world, B);
        viewers.events.clear();

        assertEquals(EditResult.CHANGED, worlds.setBlockBy(A, new BlockPosition(1090, -54, 1030), STONE));

        assertEquals(List.of("block 1090 -54 1030 = 1 to [B]"), viewers.events);
        assertEquals(OptionalInt.of(STONE), worlds.blockSeenBy(B, new BlockPosition(1090, -54, 1030)));
        assertEquals(OptionalInt.empty(), worlds.blockSeenBy(C, new BlockPosition(1090, -54, 1030)));
        assertEquals(EditResult.CLOSED, worlds.setBlockBy(C, new BlockPosition(1090, -54, 1030), GOLD));
    }

    @Test
    void sendsTheChunksAFillChangedAgain() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.addViewer(world, A);
        worlds.setBlock(world, new BlockPosition(1104, -54, 1024), STONE);
        viewers.events.clear();

        BlockBox platform = BlockBox.between(new BlockPosition(1100, -54, 1024), new BlockPosition(1110, -54, 1024));
        assertEquals(EditResult.CHANGED, worlds.fill(world, platform, STONE));
        assertEquals(EditResult.UNCHANGED, worlds.fill(world, platform, STONE));

        assertEquals(List.of("chunks [68 64, 69 64] to [A]"), viewers.events);
        assertEquals(OptionalInt.of(STONE), worlds.blockAt(world, new BlockPosition(1110, -54, 1024)));
    }

    @Test
    void letsAPlayerViewOneWorldAtATime() {
        UUID first = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        UUID second = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));

        assertEquals(ViewerResult.ADDED, worlds.addViewer(first, A));
        assertEquals(ViewerResult.ALREADY_VIEWING, worlds.addViewer(second, A));
        assertEquals(ViewerResult.ALREADY_VIEWING, worlds.addViewer(first, A));

        assertEquals(List.of("enter A 68 64 69 65"), viewers.events);
        assertEquals(Optional.of(first), worlds.worldOf(A));
        assertEquals(Set.of(A), worlds.snapshot(first).orElseThrow().viewers());
        assertEquals(Set.of(), worlds.snapshot(second).orElseThrow().viewers());
    }

    @Test
    void forgetsAViewerWhoseViewCouldNotOpen() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        viewers.withoutView.add(A);

        assertEquals(ViewerResult.NO_VIEW, worlds.addViewer(world, A));

        assertEquals(Optional.empty(), worlds.worldOf(A));
        assertEquals(Set.of(), worlds.snapshot(world).orElseThrow().viewers());
    }

    @Test
    void forgetsAViewerWhoLeftThroughTheExitPath() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.addViewer(world, A);
        worlds.addViewer(world, B);
        viewers.events.clear();

        worlds.viewerLeft(world, A);
        assertEquals(List.of(), viewers.events);
        worlds.viewerLeft(world, A);
        worlds.viewerLeft(world, B);

        assertEquals(List.of("emptied"), viewers.events);
        assertEquals(Optional.empty(), worlds.worldOf(A));
        assertEquals(Set.of(), worlds.snapshot(world).orElseThrow().viewers());
        assertEquals(List.of(world), worlds.open());
    }

    @Test
    void removesOneViewerThroughTheExitPath() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.addViewer(world, A);
        worlds.addViewer(world, B);
        viewers.events.clear();

        assertTrue(worlds.removeViewer(world, A));
        assertFalse(worlds.removeViewer(world, A));
        assertFalse(worlds.removeViewer(world, C));

        assertEquals(List.of("exit A"), viewers.events);
        assertEquals(Set.of(B), worlds.snapshot(world).orElseThrow().viewers());
        assertEquals(Optional.of(world), worlds.worldOf(B));
    }

    @Test
    void closingSendsEveryViewerBackThenForgetsTheWorld() {
        UUID world = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.addViewer(world, A);
        worlds.addViewer(world, B);
        worlds.setBlock(world, new BlockPosition(1090, -54, 1030), STONE);
        viewers.events.clear();

        assertTrue(worlds.close(world));
        assertFalse(worlds.close(world));

        assertEquals(List.of("exit A", "exit B", "emptied", "closed"), sorted(viewers.events));
        assertEquals(Optional.empty(), worlds.snapshot(world));
        assertEquals(Optional.empty(), worlds.worldOf(A));
        assertEquals(OptionalInt.empty(), worlds.blockAt(world, new BlockPosition(1090, -54, 1030)));
        assertEquals(EditResult.CLOSED, worlds.setBlock(world, new BlockPosition(1090, -54, 1030), GOLD));
        assertEquals(ViewerResult.CLOSED, worlds.addViewer(world, C));
        assertEquals(List.of(), worlds.open());
    }

    @Test
    void closesEveryWorldAtOnce() {
        created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));

        assertEquals(2, worlds.closeAll());
        assertEquals(List.of(), worlds.open());
        assertEquals(0, worlds.closeAll());
    }

    @Test
    void keepsOverlappingWorldsApart() {
        UUID first = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        UUID second = created(worlds.create(TWO_BY_TWO, HostRule.AIR_ONLY));
        worlds.addViewer(first, A);
        worlds.addViewer(second, B);
        viewers.events.clear();

        worlds.setBlockBy(A, new BlockPosition(1090, -54, 1030), STONE);

        assertEquals(List.of("block 1090 -54 1030 = 1 to []"), viewers.events);
        assertEquals(OptionalInt.of(STONE), worlds.blockSeenBy(A, new BlockPosition(1090, -54, 1030)));
        assertEquals(OptionalInt.of(FakeChunk.AIR), worlds.blockSeenBy(B, new BlockPosition(1090, -54, 1030)));
    }

    private static UUID created(Creation creation) {
        return assertInstanceOf(Creation.Created.class, creation).world();
    }

    private static List<String> sorted(List<String> events) {
        List<String> exits = new ArrayList<>(events.subList(0, 2));
        exits.sort(String::compareTo);
        List<String> all = new ArrayList<>(exits);
        all.addAll(events.subList(2, events.size()));
        return all;
    }
}

package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkViewServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final ChunkKey CHUNK = new ChunkKey(0, 0);

    private final FakeDisplay display = new FakeDisplay();
    private final List<String> changed = new ArrayList<>();
    private final ChunkViewService<String> service = new ChunkViewService<>(display, new FakeChanges(changed));

    @BeforeEach
    void trackThePlayer() {
        service.track(PLAYER);
        display.events.clear();
    }

    @Test
    void attachesATrackedPlayerOnce() {
        service.track(PLAYER);
        service.track(OTHER);

        assertEquals(List.of("attach"), display.events);
    }

    @Test
    void publishesTheVirtualViewBeforeSendingIt() {
        display.clientChunks.add(CHUNK);

        assertEquals(ShowResult.SHOWN, service.show(PLAYER, CHUNK, DebugPattern.FLAT));
        assertEquals(List.of("build 0 0", "send V00 while virtual"), display.events);
        assertEquals("V00", display.attached.get(PLAYER).snapshot().virtualPayload());
    }

    @Test
    void namesTheVirtualChunkOfAPlayer() {
        assertEquals(Optional.empty(), service.virtualChunk(PLAYER));
        assertEquals(Optional.empty(), service.virtualChunk(OTHER));

        service.show(PLAYER, CHUNK, DebugPattern.FLAT);

        assertEquals(Optional.of(CHUNK), service.virtualChunk(PLAYER));
        service.hide(PLAYER, ExitCause.COMMAND);
        assertEquals(Optional.empty(), service.virtualChunk(PLAYER));
    }

    @Test
    void buildsTheChosenPattern() {
        service.show(PLAYER, CHUNK, DebugPattern.RAISED);

        assertEquals(DebugPattern.RAISED, display.builtPattern);
    }

    @Test
    void waitsForTheServerWhenTheClientLacksTheChunk() {
        assertEquals(ShowResult.SHOWN, service.show(PLAYER, CHUNK, DebugPattern.FLAT));

        assertEquals(List.of("build 0 0"), display.events);
        assertTrue(display.attached.get(PLAYER).snapshot().hides(CHUNK));
    }

    @Test
    void refusesASecondVirtualChunk() {
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        display.events.clear();

        assertEquals(ShowResult.ALREADY_VIRTUAL, service.show(PLAYER, new ChunkKey(5, 5), DebugPattern.FLAT));
        assertEquals(List.of(), display.events);
        assertTrue(display.attached.get(PLAYER).snapshot().hides(CHUNK));
    }

    @Test
    void refusesAnUntrackedPlayer() {
        assertEquals(ShowResult.NOT_TRACKED, service.show(OTHER, CHUNK, DebugPattern.FLAT));
        assertEquals(List.of(), display.events);
    }

    @Test
    void publishesTheRealViewBeforeResendingTheChunk() {
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.COMMAND));
        assertEquals(List.of("restart flying check while real", "resend 0 0 while real"), display.events);
        assertFalse(display.attached.get(PLAYER).snapshot().isVirtual());
    }

    @Test
    void resendsNothingWhenTheClientNoLongerHasTheChunk() {
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.COMMAND));
        assertEquals(List.of("restart flying check while real"), display.events);
    }

    @Test
    void forgetsADisconnectedPlayerWithoutResending() {
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.DISCONNECT));
        assertEquals(List.of("restart flying check while real", "detach"), display.events);
        assertEquals(ShowResult.NOT_TRACKED, service.show(PLAYER, CHUNK, DebugPattern.FLAT));
    }

    @Test
    void keepsTrackingAfterAWorldChangeWithoutResending() {
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.WORLD_CHANGE));
        assertEquals(List.of("restart flying check while real"), display.events);
        assertEquals(ShowResult.SHOWN, service.show(PLAYER, CHUNK, DebugPattern.FLAT));
    }

    @Test
    void restartsTheFlyingCheckWhateverTheExit() {
        for (ExitCause cause : ExitCause.values()) {
            service.track(PLAYER);
            service.show(PLAYER, CHUNK, DebugPattern.FLAT);
            display.events.clear();

            service.hide(PLAYER, cause);

            assertEquals("restart flying check while real", display.events.getFirst(), cause.name());
        }
    }

    @Test
    void changesTheVirtualCopyAndPublishesIt() {
        service.show(PLAYER, CHUNK, DebugPattern.RAISED);
        display.events.clear();
        BlockPosition block = new BlockPosition(3, -54, 4);

        assertEquals(OptionalInt.of(0), service.blockAt(PLAYER, block));
        assertTrue(service.setBlock(PLAYER, block, 7));

        assertEquals(OptionalInt.of(7), service.blockAt(PLAYER, block));
        assertEquals(List.of("encode V00+7 while virtual"), display.events);
        assertEquals("V00+7", display.attached.get(PLAYER).snapshot().virtualPayload());
        assertEquals(CHUNK, display.attached.get(PLAYER).snapshot().virtualChunk());
    }

    @Test
    void leavesBlocksOutsideTheVirtualCopyAlone() {
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        display.events.clear();

        assertEquals(OptionalInt.empty(), service.blockAt(PLAYER, new BlockPosition(16, -54, 0)));
        assertFalse(service.setBlock(PLAYER, new BlockPosition(16, -54, 0), 7));
        assertFalse(service.setBlock(PLAYER, new BlockPosition(0, 320, 0), 7));
        assertFalse(service.setBlock(OTHER, new BlockPosition(0, -54, 0), 7));
        assertEquals(OptionalInt.empty(), service.blockAt(OTHER, new BlockPosition(0, -54, 0)));
        assertEquals(List.of(), display.events);
        assertEquals("V00", display.attached.get(PLAYER).snapshot().virtualPayload());
    }

    @Test
    void forgetsTheChangedCopyOnExit() {
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        service.setBlock(PLAYER, new BlockPosition(0, -54, 0), 7);

        service.hide(PLAYER, ExitCause.COMMAND);

        assertFalse(service.setBlock(PLAYER, new BlockPosition(0, -54, 0), 8));
        assertEquals(OptionalInt.empty(), service.blockAt(PLAYER, new BlockPosition(0, -54, 0)));
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        assertEquals("V00", display.attached.get(PLAYER).snapshot().virtualPayload());
    }

    @Test
    void reportsARealPlayerAsNotVirtual() {
        assertEquals(HideResult.NOT_VIRTUAL, service.hide(PLAYER, ExitCause.COMMAND));
        assertEquals(HideResult.NOT_VIRTUAL, service.hide(OTHER, ExitCause.COMMAND));
        assertEquals(List.of(), display.events);
    }

    @Test
    void tellsTheChangesAfterTheChunkOnEntryAndOnExit() {
        ChunkViewService<String> ordered = new ChunkViewService<>(display, new FakeChanges(display.events));
        ordered.track(PLAYER);
        display.clientChunks.add(CHUNK);
        display.events.clear();

        ordered.show(PLAYER, CHUNK, DebugPattern.FLAT);
        ordered.hide(PLAYER, ExitCause.COMMAND);

        assertEquals(List.of("build 0 0", "send V00 while virtual", "entered 0 0",
                "restart flying check while real", "resend 0 0 while real", "left COMMAND"), display.events);
    }

    @Test
    void tellsEveryExitCauseOnceAndNothingForARealPlayer() {
        for (ExitCause cause : ExitCause.values()) {
            service.track(PLAYER);
            service.show(PLAYER, CHUNK, DebugPattern.FLAT);
            service.hide(PLAYER, cause);
            service.hide(PLAYER, cause);
        }
        service.show(OTHER, CHUNK, DebugPattern.FLAT);

        assertEquals(List.of("entered 0 0", "left COMMAND", "entered 0 0", "left WORLD_CHANGE",
                "entered 0 0", "left DISCONNECT", "entered 0 0", "left SHUTDOWN"), changed);
    }

    @Test
    void tellsNoEntryForARefusedChunk() {
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        changed.clear();

        service.show(PLAYER, new ChunkKey(5, 5), DebugPattern.FLAT);

        assertEquals(List.of(), changed);
    }

    @Test
    void onlyADisconnectionMeansThePlayerLeaves() {
        assertEquals(List.of(ExitCause.DISCONNECT),
                Arrays.stream(ExitCause.values()).filter(ExitCause::playerLeaves).toList());
    }

    @Test
    void restoresEveryVirtualViewAtShutdown() {
        service.track(OTHER);
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, CHUNK, DebugPattern.FLAT);
        display.events.clear();

        assertEquals(1, service.hideAll(ExitCause.SHUTDOWN));
        assertTrue(display.events.contains("resend 0 0 while real"));
        assertEquals(2, display.events.stream().filter("detach"::equals).count());
        assertTrue(display.attached.isEmpty());
        assertEquals(0, service.hideAll(ExitCause.SHUTDOWN));
    }
}

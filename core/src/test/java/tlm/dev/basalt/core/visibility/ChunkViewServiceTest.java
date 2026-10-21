package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkViewServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final ChunkKey CHUNK = new ChunkKey(0, 0);

    private final FakeDisplay display = new FakeDisplay();
    private final ChunkViewService<String> service = new ChunkViewService<>(display);

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
    void reportsARealPlayerAsNotVirtual() {
        assertEquals(HideResult.NOT_VIRTUAL, service.hide(PLAYER, ExitCause.COMMAND));
        assertEquals(HideResult.NOT_VIRTUAL, service.hide(OTHER, ExitCause.COMMAND));
        assertEquals(List.of(), display.events);
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

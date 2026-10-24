package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChunkViewServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID SECOND_WORLD = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
    private static final ChunkKey CHUNK = new ChunkKey(0, 0);
    private static final ChunkRange ONE_CHUNK = ChunkRange.of(CHUNK);
    private static final ChunkRange TWO_BY_TWO = new ChunkRange(0, 0, 1, 1);

    private final FakeDisplay display = new FakeDisplay();
    private final List<String> changed = new ArrayList<>();
    private final ChunkViewService service = new ChunkViewService(display, new FakeChanges(changed), new ChunkRequests());

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

        assertEquals(ShowResult.SHOWN, service.show(PLAYER, WORLD, ONE_CHUNK));
        assertEquals(List.of(), display.events);
        assertEquals(1, service.sendRequested(16));

        assertEquals(List.of("send 0 0 while virtual"), display.events);
        assertEquals(WORLD, display.attached.get(PLAYER).snapshot().world());
    }

    @Test
    void namesTheWorldAndTheAreaOfAPlayer() {
        assertEquals(Optional.empty(), service.area(PLAYER));
        assertEquals(Optional.empty(), service.world(OTHER));

        service.show(PLAYER, WORLD, TWO_BY_TWO);

        assertEquals(Optional.of(TWO_BY_TWO), service.area(PLAYER));
        assertEquals(Optional.of(WORLD), service.world(PLAYER));
        service.hide(PLAYER, ExitCause.COMMAND);
        assertEquals(Optional.empty(), service.area(PLAYER));
    }

    @Test
    void sendsEveryChunkOfTheAreaTheClientHolds() {
        display.clientChunks.addAll(List.of(new ChunkKey(0, 0), new ChunkKey(1, 1), new ChunkKey(2, 2)));
        service.show(PLAYER, WORLD, TWO_BY_TWO);

        assertEquals(2, service.sendRequested(16));

        assertEquals(List.of("send 0 0 while virtual", "send 1 1 while virtual"), display.events);
        assertEquals(0, service.sendRequested(16));
    }

    @Test
    void waitsForTheServerWhenTheClientLacksTheChunk() {
        assertEquals(ShowResult.SHOWN, service.show(PLAYER, WORLD, ONE_CHUNK));
        assertEquals(0, service.sendRequested(16));

        display.clientChunks.add(CHUNK);
        service.request(PLAYER, CHUNK);
        assertEquals(1, service.sendRequested(16));

        assertEquals(List.of("send 0 0 while virtual"), display.events);
        assertTrue(display.attached.get(PLAYER).snapshot().hides(CHUNK));
    }

    @Test
    void ignoresRequestsOutsideTheAreaOrFromARealPlayer() {
        display.clientChunks.addAll(List.of(CHUNK, new ChunkKey(5, 5)));
        service.track(OTHER);
        service.show(PLAYER, WORLD, ONE_CHUNK);
        service.sendRequested(16);
        display.events.clear();

        service.request(PLAYER, new ChunkKey(5, 5));
        service.request(OTHER, CHUNK);

        assertEquals(0, service.sendRequested(16));
        assertEquals(List.of(), display.events);
    }

    @Test
    void retriesAChunkWhoseVirtualPacketIsNotReady() {
        display.clientChunks.addAll(List.of(new ChunkKey(0, 0), new ChunkKey(1, 0)));
        display.encoding.add(new ChunkKey(0, 0));
        service.show(PLAYER, WORLD, TWO_BY_TWO);

        assertEquals(1, service.sendRequested(16));
        display.encoding.clear();
        assertEquals(1, service.sendRequested(16));

        assertEquals(List.of("send 1 0 while virtual", "send 0 0 while virtual"), display.events);
        assertEquals(List.of("entered 0 0", "sent 1 0", "sent 0 0"), changed);
    }

    @Test
    void dropsAChunkTheClientForgotBeforeItsTurn() {
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, WORLD, ONE_CHUNK);
        display.clientChunks.clear();

        assertEquals(0, service.sendRequested(16));
        display.clientChunks.add(CHUNK);
        assertEquals(0, service.sendRequested(16));
        assertEquals(List.of(), display.events);
    }

    @Test
    void spreadsTheSendsOverTicksWithinTheBudget() {
        display.clientChunks.addAll(TWO_BY_TWO.chunks());
        service.show(PLAYER, WORLD, TWO_BY_TWO);

        assertEquals(3, service.sendRequested(3));
        assertEquals(1, service.sendRequested(3));
        assertEquals(0, service.sendRequested(3));
    }

    @Test
    void sharesTheBudgetBetweenPlayersInTurn() {
        display.clientChunks.addAll(TWO_BY_TWO.chunks());
        service.track(OTHER);
        service.show(PLAYER, WORLD, TWO_BY_TWO);
        service.show(OTHER, SECOND_WORLD, TWO_BY_TWO);

        assertEquals(4, service.sendRequested(4));

        assertEquals(List.of("entered 0 0", "entered 0 0", "sent 0 0", "sent 0 0", "sent 1 0", "sent 1 0"), changed);
    }

    @Test
    void forgetsPendingSendsOnExit() {
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, WORLD, ONE_CHUNK);
        service.hide(PLAYER, ExitCause.COMMAND);
        display.events.clear();

        assertEquals(0, service.sendRequested(16));
        assertEquals(List.of(), display.events);
    }

    @Test
    void refusesASecondWorld() {
        service.show(PLAYER, WORLD, ONE_CHUNK);
        display.events.clear();

        assertEquals(ShowResult.ALREADY_VIRTUAL, service.show(PLAYER, SECOND_WORLD, ChunkRange.of(new ChunkKey(5, 5))));
        assertEquals(List.of(), display.events);
        assertTrue(display.attached.get(PLAYER).snapshot().hides(CHUNK));
        assertEquals(Optional.of(WORLD), service.world(PLAYER));
    }

    @Test
    void refusesAnUntrackedPlayer() {
        assertEquals(ShowResult.NOT_TRACKED, service.show(OTHER, WORLD, ONE_CHUNK));
        assertEquals(List.of(), display.events);
    }

    @Test
    void publishesTheRealViewBeforeResendingTheChunks() {
        display.clientChunks.addAll(List.of(new ChunkKey(0, 0), new ChunkKey(1, 1)));
        service.show(PLAYER, WORLD, TWO_BY_TWO);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.COMMAND));
        assertEquals(List.of("restart flying check while real", "resend 0 0 while real", "resend 1 1 while real"), display.events);
        assertFalse(display.attached.get(PLAYER).snapshot().isVirtual());
    }

    @Test
    void resendsNothingWhenTheClientNoLongerHasTheChunk() {
        service.show(PLAYER, WORLD, ONE_CHUNK);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.COMMAND));
        assertEquals(List.of("restart flying check while real"), display.events);
    }

    @Test
    void forgetsADisconnectedPlayerWithoutResending() {
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, WORLD, ONE_CHUNK);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.DISCONNECT));
        assertEquals(List.of("restart flying check while real", "detach"), display.events);
        assertEquals(ShowResult.NOT_TRACKED, service.show(PLAYER, WORLD, ONE_CHUNK));
    }

    @Test
    void keepsTrackingAfterAWorldChangeWithoutResending() {
        display.clientChunks.add(CHUNK);
        service.show(PLAYER, WORLD, ONE_CHUNK);
        display.events.clear();

        assertEquals(HideResult.RESTORED, service.hide(PLAYER, ExitCause.WORLD_CHANGE));
        assertEquals(List.of("restart flying check while real"), display.events);
        assertEquals(ShowResult.SHOWN, service.show(PLAYER, WORLD, ONE_CHUNK));
    }

    @Test
    void restartsTheFlyingCheckWhateverTheExit() {
        for (ExitCause cause : ExitCause.values()) {
            service.track(PLAYER);
            service.show(PLAYER, WORLD, ONE_CHUNK);
            display.events.clear();

            service.hide(PLAYER, cause);

            assertEquals("restart flying check while real", display.events.getFirst(), cause.name());
        }
    }

    @Test
    void publishesTheEntitiesOfAVirtualPlayerOnly() {
        assertFalse(service.publishEntities(PLAYER, Set.of(1001)));
        assertFalse(service.publishEntities(OTHER, Set.of(1001)));

        service.show(PLAYER, WORLD, ONE_CHUNK);

        assertTrue(service.publishEntities(PLAYER, Set.of(1001)));
        assertTrue(display.attached.get(PLAYER).snapshot().holdsEntity(1001));
        service.hide(PLAYER, ExitCause.COMMAND);
        assertFalse(display.attached.get(PLAYER).snapshot().holdsEntity(1001));
    }

    @Test
    void reportsARealPlayerAsNotVirtual() {
        assertEquals(HideResult.NOT_VIRTUAL, service.hide(PLAYER, ExitCause.COMMAND));
        assertEquals(HideResult.NOT_VIRTUAL, service.hide(OTHER, ExitCause.COMMAND));
        assertEquals(List.of(), display.events);
    }

    @Test
    void tellsTheChangesAfterTheChunkOnEntryAndOnExit() {
        ChunkViewService ordered = new ChunkViewService(display, new FakeChanges(display.events), new ChunkRequests());
        ordered.track(PLAYER);
        display.clientChunks.add(CHUNK);
        display.events.clear();

        ordered.show(PLAYER, WORLD, ONE_CHUNK);
        ordered.sendRequested(16);
        ordered.hide(PLAYER, ExitCause.COMMAND);

        assertEquals(List.of("entered 0 0", "send 0 0 while virtual", "sent 0 0",
                "restart flying check while real", "resend 0 0 while real", "left COMMAND"), display.events);
    }

    @Test
    void tellsEveryExitCauseOnceAndNothingForARealPlayer() {
        for (ExitCause cause : ExitCause.values()) {
            service.track(PLAYER);
            service.show(PLAYER, WORLD, ONE_CHUNK);
            service.hide(PLAYER, cause);
            service.hide(PLAYER, cause);
        }
        service.show(OTHER, WORLD, ONE_CHUNK);

        assertEquals(List.of("entered 0 0", "left COMMAND", "entered 0 0", "left WORLD_CHANGE",
                "entered 0 0", "left DISCONNECT", "entered 0 0", "left SHUTDOWN"), changed);
    }

    @Test
    void tellsNoEntryForARefusedWorld() {
        service.show(PLAYER, WORLD, ONE_CHUNK);
        changed.clear();

        service.show(PLAYER, SECOND_WORLD, ChunkRange.of(new ChunkKey(5, 5)));

        assertEquals(List.of(), changed);
    }

    @Test
    void tellsBothListenersInOrder() {
        List<String> heard = new ArrayList<>();
        ViewChanges both = ViewChanges.both(new FakeChanges(heard), new FakeChanges(heard));

        both.entered(PLAYER, WORLD, ONE_CHUNK);
        both.sent(PLAYER, WORLD, CHUNK);
        both.left(PLAYER, WORLD, ExitCause.COMMAND);

        assertEquals(List.of("entered 0 0", "entered 0 0", "sent 0 0", "sent 0 0", "left COMMAND", "left COMMAND"), heard);
    }

    @Test
    void releasesTheChunksOfAWorldThroughTheDisplay() {
        service.release(WORLD);

        assertEquals(List.of("release"), display.events);
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
        service.show(PLAYER, WORLD, ONE_CHUNK);
        display.events.clear();

        assertEquals(1, service.hideAll(ExitCause.SHUTDOWN));
        assertTrue(display.events.contains("resend 0 0 while real"));
        assertEquals(2, display.events.stream().filter("detach"::equals).count());
        assertTrue(display.attached.isEmpty());
        assertEquals(0, service.hideAll(ExitCause.SHUTDOWN));
    }
}

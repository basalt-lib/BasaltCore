package tlm.dev.basalt.core.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.position.Point;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualEntityServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID OVERLAPPING = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
    private static final ChunkKey ARENA = new ChunkKey(64, 64);
    private static final Point INSIDE = new Point(1030.5, -52, 1028.5);
    private static final Point ALSO_INSIDE = new Point(1035.5, -52, 1031.5);
    private static final Point OUTSIDE = new Point(1044.5, -52, 1028.5);

    private final AtomicInteger counter = new AtomicInteger(1000);
    private final FakeEntityDisplay display = new FakeEntityDisplay();
    private final FakeEntityViews views = new FakeEntityViews();
    private final VirtualEntityService service = new VirtualEntityService(
            new EntityTools(new VirtualEntityIds(counter::incrementAndGet), display, views), new EntitySettings(3));

    @BeforeEach
    void enterTheArena() {
        views.areas.put(WORLD, ChunkRange.of(ARENA));
        views.areas.put(OVERLAPPING, ChunkRange.of(ARENA));
        views.viewing.put(PLAYER, WORLD);
    }

    @Test
    void spawnsTheDebugZombieAndPublishesItsId() {
        EntityResult result = service.spawn(PLAYER, INSIDE);

        assertEquals(1001, result.entity().orElseThrow().id());
        assertEquals(List.of("A: spawn #1001 minecraft:zombie health 5"), display.events);
        assertEquals(Set.of(1001), views.published.get(WORLD));
    }

    @Test
    void spawnsOnlyInsideTheAreaOfTheWorldOfThePlayer() {
        assertEquals(Optional.of(EntityRefusal.OUTSIDE_VIRTUAL_CHUNK), service.spawn(PLAYER, OUTSIDE).refusal());
        assertEquals(Optional.of(EntityRefusal.NOT_VIRTUAL), service.spawn(OTHER, INSIDE).refusal());
        assertTrue(display.events.isEmpty());
        assertEquals(1000, counter.get());
    }

    @Test
    void boundsTheEntitiesOfAWorld() {
        views.viewing.put(SECOND, WORLD);
        service.spawn(PLAYER, INSIDE);
        service.spawn(SECOND, INSIDE);
        service.spawn(PLAYER, INSIDE);

        assertEquals(Optional.of(EntityRefusal.TOO_MANY), service.spawn(SECOND, INSIDE).refusal());
        assertEquals(3, service.status(PLAYER).entities());
        assertEquals(3, service.status(SECOND).entities());
    }

    @Test
    void movesTheNewestEntityInsideTheArea() {
        service.spawn(PLAYER, INSIDE);
        service.spawn(PLAYER, INSIDE);
        display.events.clear();

        assertEquals(ALSO_INSIDE, service.move(PLAYER, ALSO_INSIDE).entity().orElseThrow().position());
        assertEquals(Optional.of(EntityRefusal.OUTSIDE_VIRTUAL_CHUNK), service.move(PLAYER, OUTSIDE).refusal());
        assertEquals(Optional.of(EntityRefusal.NONE_TO_MOVE), service.move(OTHER, INSIDE).refusal());
        assertEquals(List.of("A: move #1002 to 1035.5 1031.5"), display.events);
    }

    @Test
    void fiveSpacedHitsKillTheZombieThenItIsRemovedTwentyTicksLater() {
        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();
        display.events.clear();

        for (int hit = 0; hit < 4; hit++) {
            assertEquals(HitOutcome.HURT, service.attack(PLAYER, id));
            advance(DebugEntityPattern.HIT_COOLDOWN_TICKS);
        }
        assertEquals(HitOutcome.KILLED, service.attack(PLAYER, id));
        assertEquals(HitOutcome.GONE, service.attack(PLAYER, id));
        assertFalse(service.use(PLAYER, id));
        assertEquals(Optional.empty(), service.find(PLAYER, id));

        advance(DebugEntityPattern.REMOVAL_AFTER_DEATH_TICKS - 1);
        assertEquals(Set.of(id), views.published.get(WORLD));
        advance(1);

        assertEquals(List.of("A: hurt #1001 health 4 by A", "A: hurt #1001 health 3 by A", "A: hurt #1001 health 2 by A",
                "A: hurt #1001 health 1 by A", "A: die #1001 by A", "A: remove dead [1001]"), display.events);
        assertEquals(Set.of(), views.published.get(WORLD));
        assertEquals(new EntityStatus(0, Optional.empty(), 5, 0), service.status(PLAYER));
    }

    @Test
    void refusesHitsInsideTheCooldown() {
        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();

        assertEquals(HitOutcome.HURT, service.attack(PLAYER, id));
        advance(DebugEntityPattern.HIT_COOLDOWN_TICKS - 1);
        assertEquals(HitOutcome.TOO_SOON, service.attack(PLAYER, id));
        advance(1);
        assertEquals(HitOutcome.HURT, service.attack(PLAYER, id));
        assertEquals(2, service.status(PLAYER).hits());
        assertEquals(3, service.find(PLAYER, id).orElseThrow().health());
    }

    @Test
    void aPlayerOutsideTheWorldCannotReachItsEntities() {
        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();

        assertEquals(HitOutcome.GONE, service.attack(OTHER, id));
        assertFalse(service.use(OTHER, id));
        assertEquals(Optional.empty(), service.find(OTHER, id));
        assertEquals(HitOutcome.GONE, service.attack(PLAYER, id + 1));
    }

    @Test
    void countsUsesWithoutAnyOtherEffect() {
        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();
        display.events.clear();

        assertTrue(service.use(PLAYER, id));
        assertTrue(service.use(PLAYER, id));

        EntityStatus status = service.status(PLAYER);
        assertEquals(2, status.uses());
        assertEquals(0, status.hits());
        assertEquals(5, status.last().orElseThrow().health());
        assertEquals(List.of(), display.events);
    }

    @Test
    void removesEverythingOnceAndForgetsAClosedWorldWithoutPackets() {
        service.spawn(PLAYER, INSIDE);
        service.spawn(PLAYER, INSIDE);
        display.events.clear();

        assertEquals(2, service.held());
        assertEquals(2, service.removeAll(PLAYER));
        assertEquals(0, service.held());
        assertEquals(0, service.removeAll(PLAYER));
        assertEquals(List.of("A: remove [1001, 1002]"), display.events);
        assertEquals(Set.of(), views.published.get(WORLD));
        assertEquals(0, service.worldsTracked());

        service.spawn(PLAYER, INSIDE);
        display.events.clear();
        service.forgetWorld(WORLD);
        assertEquals(List.of(), display.events);
        assertEquals(0, service.worldsTracked());
    }

    @Test
    void resendsTheLivingEntitiesOfTheSentChunkOnly() {
        views.areas.put(WORLD, new ChunkRange(64, 64, 65, 64));
        int first = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();
        service.spawn(PLAYER, ALSO_INSIDE);
        service.spawn(PLAYER, OUTSIDE);
        for (int hit = 0; hit < DebugEntityPattern.HEALTH; hit++) {
            service.attack(PLAYER, first);
            advance(DebugEntityPattern.HIT_COOLDOWN_TICKS);
        }
        display.events.clear();

        service.resend(PLAYER, WORLD, ARENA);
        service.resend(PLAYER, OVERLAPPING, ARENA);

        assertEquals(List.of("A: spawn #1002 minecraft:zombie health 5"), display.events);
    }

    @Test
    void showsAnEntityToEveryViewerOfItsWorld() {
        views.viewing.put(SECOND, WORLD);
        views.viewing.put(OTHER, OVERLAPPING);

        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();
        service.move(SECOND, ALSO_INSIDE);

        assertEquals(List.of("A: spawn #1001 minecraft:zombie health 5", "B: spawn #1001 minecraft:zombie health 5",
                "A: move #1001 to 1035.5 1031.5", "B: move #1001 to 1035.5 1031.5"), display.events);
        assertEquals(Optional.of(id), service.find(SECOND, id).map(VirtualEntity::id));
        assertEquals(Optional.empty(), service.find(OTHER, id));
    }

    @Test
    void aHitFromOneViewerHurtsAndKillsTheEntityForAll() {
        views.viewing.put(SECOND, WORLD);
        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();
        display.events.clear();

        service.attack(PLAYER, id);
        for (int hit = 1; hit < DebugEntityPattern.HEALTH; hit++) {
            advance(DebugEntityPattern.HIT_COOLDOWN_TICKS);
            service.attack(SECOND, id);
        }
        advance(DebugEntityPattern.REMOVAL_AFTER_DEATH_TICKS);

        assertEquals(List.of("A: hurt #1001 health 4 by A", "B: hurt #1001 health 4 by A"), display.events.subList(0, 2));
        assertEquals(List.of("A: die #1001 by B", "B: die #1001 by B", "A: remove dead [1001]", "B: remove dead [1001]"),
                display.events.subList(display.events.size() - 4, display.events.size()));
        assertEquals(5, service.status(PLAYER).hits());
    }

    @Test
    void keepsTheDelayBetweenTwoHitsPerViewer() {
        views.viewing.put(SECOND, WORLD);
        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();

        assertEquals(HitOutcome.HURT, service.attack(PLAYER, id));
        assertEquals(HitOutcome.HURT, service.attack(SECOND, id));
        assertEquals(HitOutcome.TOO_SOON, service.attack(PLAYER, id));
        assertEquals(HitOutcome.TOO_SOON, service.attack(SECOND, id));
        assertEquals(3, service.find(PLAYER, id).orElseThrow().health());
    }

    @Test
    void sendsTheRemovalToALeavingViewerThatKeepsItsChunksOnly() {
        views.viewing.put(SECOND, WORLD);
        service.spawn(PLAYER, INSIDE);
        service.spawn(PLAYER, INSIDE);
        display.events.clear();

        service.viewerLeft(PLAYER, WORLD, true);
        service.viewerLeft(SECOND, WORLD, false);
        service.viewerLeft(OTHER, OVERLAPPING, true);

        assertEquals(List.of("A: remove [1001, 1002]"), display.events);
        assertEquals(2, service.held());
    }

    @Test
    void publishesTheIdsAgainForANewViewer() {
        service.spawn(PLAYER, INSIDE);
        views.published.clear();

        service.viewerEntered(WORLD);
        service.viewerEntered(OVERLAPPING);

        assertEquals(Set.of(1001), views.published.get(WORLD));
        assertFalse(views.published.containsKey(OVERLAPPING));
    }

    @Test
    void keepsTheEntitiesOfOverlappingWorldsApart() {
        views.viewing.put(SECOND, OVERLAPPING);

        int id = service.spawn(PLAYER, INSIDE).entity().orElseThrow().id();

        assertEquals(List.of("A: spawn #1001 minecraft:zombie health 5"), display.events);
        assertEquals(HitOutcome.GONE, service.attack(SECOND, id));
        assertEquals(0, service.status(SECOND).entities());
    }

    private void advance(int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            service.tick();
        }
    }
}

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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualEntityServiceTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000c");
    private static final UUID WORLD = UUID.fromString("00000000-0000-0000-0000-0000000000f1");
    private static final UUID OVERLAPPING = UUID.fromString("00000000-0000-0000-0000-0000000000f2");
    private static final UUID UNKNOWN = UUID.fromString("00000000-0000-0000-0000-0000000000f3");
    private static final String ZOMBIE = "minecraft:zombie";
    private static final ChunkKey ARENA = new ChunkKey(64, 64);
    private static final ChunkKey EAST = new ChunkKey(65, 64);
    private static final Point INSIDE = new Point(1030.5, -52, 1028.5);
    private static final Point ALSO_INSIDE = new Point(1035.5, -52, 1031.5);
    private static final Point OUTSIDE = new Point(1044.5, -52, 1028.5);

    private final AtomicInteger counter = new AtomicInteger(1000);
    private final FakeEntityDisplay display = new FakeEntityDisplay();
    private final FakeEntityViews views = new FakeEntityViews();
    private final FakeEntityEvents events = new FakeEntityEvents();
    private final VirtualEntityService service = new VirtualEntityService(
            new EntityTools(new VirtualEntityIds(counter::incrementAndGet), display, views, events), new EntitySettings(3));

    @BeforeEach
    void enterTheArena() {
        views.areas.put(WORLD, ChunkRange.of(ARENA));
        views.areas.put(OVERLAPPING, ChunkRange.of(ARENA));
        views.viewing.put(PLAYER, WORLD);
    }

    @Test
    void showsANewEntityAtTheNextTickToTheViewersHoldingItsChunk() {
        views.viewing.put(SECOND, WORLD);
        views.lacks(SECOND, ARENA);

        EntityResult result = service.spawn(WORLD, ZOMBIE, INSIDE, Rotation.NONE);

        assertEquals(1001, result.entity().orElseThrow().id());
        assertEquals(Set.of(1001), views.published.get(WORLD));
        assertEquals(List.of(), display.events);
        service.tick();
        service.tick();
        assertEquals(List.of("A: spawn #1001 minecraft:zombie health 20.0/20.0"), display.events);

        service.resend(SECOND, WORLD, ARENA);
        assertEquals("B: spawn #1001 minecraft:zombie health 20.0/20.0", display.events.getLast());
    }

    @Test
    void sendsTheFinalStateOfAnEntityConfiguredBeforeItIsShown() {
        int id = spawn(INSIDE);
        service.rename(WORLD, id, Optional.of("{\"text\":\"Basalt\"}"));
        service.limitHealth(WORLD, id, 5);
        service.silence(WORLD, id, true);
        service.teleport(WORLD, id, ALSO_INSIDE, new Rotation(90, 0));

        service.tick();

        assertEquals(List.of("A: spawn #1001 minecraft:zombie health 5.0/5.0 named {\"text\":\"Basalt\"} silent"), display.events);
        assertEquals(ALSO_INSIDE, service.entity(WORLD, id).orElseThrow().position());
    }

    @Test
    void neverShowsAnEntityRemovedBeforeTheNextTick() {
        int id = spawn(INSIDE);

        assertTrue(service.remove(WORLD, id));
        service.tick();

        assertEquals(List.of(), display.events);
        assertEquals(Set.of(), views.published.get(WORLD));
    }

    @Test
    void spawnsOnlyInsideTheAreaOfAnOpenWorld() {
        assertEquals(Optional.of(EntityRefusal.OUTSIDE_AREA), service.spawn(WORLD, ZOMBIE, OUTSIDE, Rotation.NONE).refusal());
        assertEquals(Optional.of(EntityRefusal.NO_WORLD), service.spawn(UNKNOWN, ZOMBIE, INSIDE, Rotation.NONE).refusal());
        service.tick();
        assertTrue(display.events.isEmpty());
        assertEquals(1000, counter.get());
    }

    @Test
    void boundsTheEntitiesOfAWorld() {
        int first = spawn(INSIDE);
        spawn(INSIDE);
        spawn(INSIDE);

        assertEquals(Optional.of(EntityRefusal.TOO_MANY), service.spawn(WORLD, ZOMBIE, INSIDE, Rotation.NONE).refusal());
        assertEquals(3, service.status(PLAYER).entities());
        service.remove(WORLD, first);
        assertTrue(service.spawn(WORLD, ZOMBIE, INSIDE, Rotation.NONE).entity().isPresent());
    }

    @Test
    void listsTheEntitiesOfAWorldInCreationOrder() {
        int first = spawn(INSIDE);
        int second = spawn(ALSO_INSIDE);

        assertEquals(List.of(first, second), service.entities(WORLD).stream().map(VirtualEntity::id).toList());
        assertEquals(Optional.of(ALSO_INSIDE), service.entity(WORLD, second).map(VirtualEntity::position));
        assertEquals(List.of(), service.entities(OVERLAPPING));
        assertEquals(Optional.empty(), service.entity(OVERLAPPING, first));
    }

    @Test
    void teleportsForEveryViewerHoldingTheChunkBeforeOrAfter() {
        views.areas.put(WORLD, new ChunkRange(64, 64, 65, 64));
        views.viewing.put(SECOND, WORLD);
        views.viewing.put(OTHER, WORLD);
        views.lacks(SECOND, ARENA);
        views.lacks(OTHER, EAST);
        int id = shown(INSIDE);
        Point east = new Point(1045.5, -52, 1028.5);

        assertEquals(east, service.teleport(WORLD, id, east, Rotation.NONE).entity().orElseThrow().position());

        assertEquals(List.of("A: move #1001 to 1045.5 1028.5", "B: spawn #1001 minecraft:zombie health 20.0/20.0",
                "C: move #1001 to 1045.5 1028.5"), display.events);
        assertEquals(Optional.of(EntityRefusal.OUTSIDE_AREA), service.teleport(WORLD, id, new Point(1060.5, -52, 1028.5), Rotation.NONE).refusal());
        assertEquals(Optional.of(EntityRefusal.GONE), service.teleport(WORLD, id + 1, INSIDE, Rotation.NONE).refusal());
    }

    @Test
    void updatesTheLooksAndTheLifeOfAShownEntity() {
        int id = shown(INSIDE);

        service.rename(WORLD, id, Optional.of("{\"text\":\"Boss\"}"));
        service.silence(WORLD, id, true);
        service.limitHealth(WORLD, id, 10);
        service.heal(WORLD, id, 4);
        service.rename(WORLD, id, Optional.empty());

        assertEquals(List.of("A: update #1001 minecraft:zombie health 20.0/20.0 named {\"text\":\"Boss\"}",
                "A: update #1001 minecraft:zombie health 20.0/20.0 named {\"text\":\"Boss\"} silent",
                "A: update #1001 minecraft:zombie health 10.0/10.0 named {\"text\":\"Boss\"} silent",
                "A: update #1001 minecraft:zombie health 4.0/10.0 named {\"text\":\"Boss\"} silent",
                "A: update #1001 minecraft:zombie health 4.0/10.0 silent"), display.events);
        assertThrows(IllegalArgumentException.class, () -> service.heal(WORLD, id, 11));
        assertThrows(IllegalArgumentException.class, () -> service.limitHealth(WORLD, id, 0));
        assertEquals(Optional.of(EntityRefusal.GONE), service.heal(WORLD, id + 1, 1).refusal());
    }

    @Test
    void healthSetToZeroKillsTheEntityWithoutKiller() {
        int id = shown(INSIDE);

        service.heal(WORLD, id, 0);

        assertEquals(List.of("died #1001 by none"), events.deaths);
        assertEquals(List.of("A: die #1001 by none"), display.events);
        assertEquals(Optional.of(EntityRefusal.DYING), service.heal(WORLD, id, 5).refusal());
        assertEquals(Optional.of(EntityRefusal.DYING), service.limitHealth(WORLD, id, 5).refusal());
        assertTrue(service.rename(WORLD, id, Optional.of("{\"text\":\"Gone\"}")).entity().isPresent());
        advance(VirtualEntityService.REMOVAL_AFTER_DEATH_TICKS);
        assertEquals("A: remove dead [1001]", display.events.getLast());
        assertEquals(List.of(), service.entities(WORLD));
    }

    @Test
    void fiveSpacedHitsKillAFiveHealthEntityThenItIsRemovedTwentyTicksLater() {
        int id = shown(INSIDE);
        service.limitHealth(WORLD, id, 5);
        display.events.clear();

        for (int hit = 0; hit < 4; hit++) {
            assertEquals(HitOutcome.HURT, service.attack(PLAYER, id, 1));
            advance(VirtualEntityService.HIT_COOLDOWN_TICKS);
        }
        assertEquals(HitOutcome.KILLED, service.attack(PLAYER, id, 1));
        assertEquals(HitOutcome.GONE, service.attack(PLAYER, id, 1));
        assertFalse(service.use(PLAYER, id));
        assertEquals(Optional.empty(), service.find(PLAYER, id));

        advance(VirtualEntityService.REMOVAL_AFTER_DEATH_TICKS - 1);
        assertEquals(Set.of(id), views.published.get(WORLD));
        advance(1);

        assertEquals(List.of("A: hurt #1001 health 4.0 by A", "A: hurt #1001 health 3.0 by A", "A: hurt #1001 health 2.0 by A",
                "A: hurt #1001 health 1.0 by A", "A: die #1001 by A", "A: remove dead [1001]"), display.events);
        assertEquals(List.of("died #1001 by A"), events.deaths);
        assertEquals(Set.of(), views.published.get(WORLD));
        assertEquals(new EntityStatus(0, Optional.empty(), 5, 0), service.status(PLAYER));
    }

    @Test
    void hitsWithTheGivenDamage() {
        int id = shown(INSIDE);

        service.attack(PLAYER, id, 7.5);
        advance(VirtualEntityService.HIT_COOLDOWN_TICKS);
        service.attack(PLAYER, id, 0);

        assertEquals(12.5, service.entity(WORLD, id).orElseThrow().health());
        assertEquals(List.of("A: hurt #1001 health 12.5 by A", "A: hurt #1001 health 12.5 by A"), display.events);
    }

    @Test
    void refusesHitsInsideTheCooldown() {
        int id = shown(INSIDE);

        assertTrue(service.hitReady(PLAYER, id));
        assertEquals(HitOutcome.HURT, service.attack(PLAYER, id, 1));
        advance(VirtualEntityService.HIT_COOLDOWN_TICKS - 1);
        assertFalse(service.hitReady(PLAYER, id));
        assertEquals(HitOutcome.TOO_SOON, service.attack(PLAYER, id, 1));
        advance(1);
        assertTrue(service.hitReady(PLAYER, id));
        assertEquals(HitOutcome.HURT, service.attack(PLAYER, id, 1));
        assertEquals(2, service.status(PLAYER).hits());
        assertEquals(18, service.find(PLAYER, id).orElseThrow().health());
    }

    @Test
    void aPlayerOutsideTheWorldCannotReachItsEntities() {
        int id = shown(INSIDE);

        assertEquals(HitOutcome.GONE, service.attack(OTHER, id, 1));
        assertFalse(service.hitReady(OTHER, id));
        assertFalse(service.use(OTHER, id));
        assertEquals(Optional.empty(), service.find(OTHER, id));
        assertEquals(HitOutcome.GONE, service.attack(PLAYER, id + 1, 1));
    }

    @Test
    void countsUsesWithoutAnyOtherEffect() {
        int id = shown(INSIDE);

        assertTrue(service.use(PLAYER, id));
        assertTrue(service.use(PLAYER, id));

        EntityStatus status = service.status(PLAYER);
        assertEquals(2, status.uses());
        assertEquals(0, status.hits());
        assertEquals(20, status.last().orElseThrow().health());
        assertEquals(List.of(), display.events);
    }

    @Test
    void removesAShownEntityOnceForEveryViewer() {
        views.viewing.put(SECOND, WORLD);
        views.lacks(SECOND, ARENA);
        int id = shown(INSIDE);

        assertTrue(service.remove(WORLD, id));
        assertFalse(service.remove(WORLD, id));
        assertFalse(service.remove(UNKNOWN, id));

        assertEquals(List.of("A: remove [1001]", "B: remove [1001]"), display.events);
        assertEquals(Set.of(), views.published.get(WORLD));
        assertEquals(0, service.held());
    }

    @Test
    void forgetsAClosedWorldWithoutPackets() {
        shown(INSIDE);
        shown(INSIDE);

        service.forgetWorld(WORLD);

        assertEquals(List.of(), display.events);
        assertEquals(0, service.held());
        assertEquals(List.of(), service.entities(WORLD));
    }

    @Test
    void resendsTheShownLivingEntitiesOfTheSentChunkOnly() {
        views.areas.put(WORLD, new ChunkRange(64, 64, 65, 64));
        int first = shown(INSIDE);
        shown(ALSO_INSIDE);
        spawn(INSIDE);
        service.heal(WORLD, first, 0);
        display.events.clear();

        service.resend(PLAYER, WORLD, ARENA);
        service.resend(PLAYER, WORLD, EAST);
        service.resend(PLAYER, OVERLAPPING, ARENA);

        assertEquals(List.of("A: spawn #1002 minecraft:zombie health 20.0/20.0"), display.events);
    }

    @Test
    void aHitFromOneViewerHurtsAndKillsTheEntityForAll() {
        views.viewing.put(SECOND, WORLD);
        int id = shown(INSIDE);
        service.limitHealth(WORLD, id, 5);
        display.events.clear();

        service.attack(PLAYER, id, 1);
        for (int hit = 1; hit < 5; hit++) {
            advance(VirtualEntityService.HIT_COOLDOWN_TICKS);
            service.attack(SECOND, id, 1);
        }
        advance(VirtualEntityService.REMOVAL_AFTER_DEATH_TICKS);

        assertEquals(List.of("A: hurt #1001 health 4.0 by A", "B: hurt #1001 health 4.0 by A"), display.events.subList(0, 2));
        assertEquals(List.of("A: die #1001 by B", "B: die #1001 by B", "A: remove dead [1001]", "B: remove dead [1001]"),
                display.events.subList(display.events.size() - 4, display.events.size()));
        assertEquals(5, service.status(PLAYER).hits());
    }

    @Test
    void aDeathHandlerMayRemoveTheEntityBeforeItsDeathIsShown() {
        int id = shown(INSIDE);
        events.onDeath = dead -> service.remove(WORLD, dead.id());

        assertEquals(HitOutcome.KILLED, service.attack(PLAYER, id, 20));
        advance(VirtualEntityService.REMOVAL_AFTER_DEATH_TICKS);

        assertEquals(List.of("A: remove [1001]"), display.events);
        assertEquals(List.of("died #1001 by A"), events.deaths);
    }

    @Test
    void keepsTheDelayBetweenTwoHitsPerViewer() {
        views.viewing.put(SECOND, WORLD);
        int id = shown(INSIDE);

        assertEquals(HitOutcome.HURT, service.attack(PLAYER, id, 1));
        assertEquals(HitOutcome.HURT, service.attack(SECOND, id, 1));
        assertEquals(HitOutcome.TOO_SOON, service.attack(PLAYER, id, 1));
        assertEquals(HitOutcome.TOO_SOON, service.attack(SECOND, id, 1));
        assertEquals(18, service.find(PLAYER, id).orElseThrow().health());
    }

    @Test
    void sendsTheRemovalToALeavingViewerThatKeepsItsChunksOnly() {
        views.viewing.put(SECOND, WORLD);
        shown(INSIDE);
        shown(INSIDE);
        spawn(INSIDE);
        display.events.clear();

        service.viewerLeft(PLAYER, WORLD, true);
        service.viewerLeft(SECOND, WORLD, false);
        service.viewerLeft(OTHER, OVERLAPPING, true);

        assertEquals(List.of("A: remove [1001, 1002]"), display.events);
        assertEquals(3, service.held());
    }

    @Test
    void publishesTheIdsAgainForANewViewer() {
        spawn(INSIDE);
        views.published.clear();

        service.viewerEntered(WORLD);
        service.viewerEntered(OVERLAPPING);

        assertEquals(Set.of(1001), views.published.get(WORLD));
        assertFalse(views.published.containsKey(OVERLAPPING));
    }

    @Test
    void keepsTheEntitiesOfOverlappingWorldsApart() {
        views.viewing.put(SECOND, OVERLAPPING);

        int id = spawn(INSIDE);
        service.tick();

        assertEquals(List.of("A: spawn #1001 minecraft:zombie health 20.0/20.0"), display.events);
        assertEquals(HitOutcome.GONE, service.attack(SECOND, id, 1));
        assertEquals(0, service.status(SECOND).entities());
        assertEquals(1, service.status(PLAYER).entities());
    }

    private int spawn(Point at) {
        return service.spawn(WORLD, ZOMBIE, at, Rotation.NONE).entity().orElseThrow().id();
    }

    private int shown(Point at) {
        int id = spawn(at);
        service.tick();
        display.events.clear();
        return id;
    }

    private void advance(int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            service.tick();
        }
    }
}

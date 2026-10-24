package tlm.dev.basalt.core.entity;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.Point;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualEntityTest {

    private static final Point AT = new Point(1030.5, -52, 1028.5);

    @Test
    void takesEveryIdFromItsSourceAndNeverGoesBack() {
        AtomicInteger counter = new AtomicInteger(400);
        VirtualEntityIds ids = new VirtualEntityIds(counter::incrementAndGet);

        assertEquals(401, ids.next());
        counter.incrementAndGet();
        assertEquals(403, ids.next());
        counter.set(100);
        assertThrows(IllegalStateException.class, ids::next);
    }

    @Test
    void refusesAnIdThatIsNotPositive() {
        assertThrows(IllegalStateException.class, () -> new VirtualEntityIds(() -> 0).next());
        assertThrows(IllegalStateException.class, () -> new VirtualEntityIds(() -> -7).next());
    }

    @Test
    void aFreshEntityHasNoNameAndTwentyHealthAndAStableUuid() {
        VirtualEntity zombie = VirtualEntity.fresh(42, "minecraft:zombie", AT, Rotation.NONE);

        assertEquals("minecraft:zombie", zombie.type());
        assertEquals(EntityLooks.PLAIN, zombie.looks());
        assertEquals(20, zombie.health());
        assertEquals(20, zombie.life().maxHealth());
        assertEquals(AT, zombie.position());
        assertEquals(VirtualEntity.fresh(42, "minecraft:cow", AT, Rotation.NONE).uuid(), zombie.uuid());
        assertNotEquals(VirtualEntity.fresh(43, "minecraft:zombie", AT, Rotation.NONE).uuid(), zombie.uuid());
    }

    @Test
    void diesAtZeroHealthAndNeverBelow() {
        VirtualEntity zombie = VirtualEntity.fresh(42, "minecraft:zombie", AT, Rotation.NONE);

        VirtualEntity wounded = zombie.hurt(19.5);
        assertFalse(wounded.isDead());
        assertTrue(wounded.hurt(0.5).isDead());
        assertEquals(0, wounded.hurt(9).health());
        assertEquals(20, zombie.health());
        assertThrows(IllegalArgumentException.class, () -> zombie.hurt(-1));
        assertThrows(IllegalArgumentException.class, () -> zombie.hurt(Double.NaN));
    }

    @Test
    void aLowerMaxHealthLowersTheHealth() {
        EntityLife life = EntityLife.full(20).withHealth(12);

        assertEquals(new EntityLife(5, 5), life.withMaxHealth(5));
        assertEquals(new EntityLife(12, 30), life.withMaxHealth(30));
        assertThrows(IllegalArgumentException.class, () -> life.withHealth(21));
        assertThrows(IllegalArgumentException.class, () -> life.withMaxHealth(0));
        assertThrows(IllegalArgumentException.class, () -> life.withMaxHealth(EntityLife.MAX_HEALTH_LIMIT + 1));
    }

    @Test
    void movingAndRestylingKeepEverythingElse() {
        VirtualEntity zombie = VirtualEntity.fresh(42, "minecraft:zombie", AT, Rotation.NONE);
        Point there = new Point(1033.5, -52, 1030.5);

        VirtualEntity moved = zombie.movedTo(there, new Rotation(90, 10));
        VirtualEntity named = moved.withLooks(moved.looks().withName(Optional.of("{\"text\":\"Basalt\"}")).withSilent(true));

        assertEquals(there, named.position());
        assertEquals(new Rotation(90, 10), named.rotation());
        assertEquals(zombie.id(), named.id());
        assertEquals(zombie.uuid(), named.uuid());
        assertEquals(zombie.life(), named.life());
        assertEquals(new EntityLooks(Optional.of("{\"text\":\"Basalt\"}"), true), named.looks());
    }

    @Test
    void validatesItsFields() {
        UUID uuid = UUID.randomUUID();
        EntityLife life = EntityLife.full(5);
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(0, uuid, "minecraft:zombie", AT, Rotation.NONE, EntityLooks.PLAIN, life));
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(1, uuid, "Zombie", AT, Rotation.NONE, EntityLooks.PLAIN, life));
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(1, uuid, "minecraft:zombie", new Point(Double.NaN, 0, 0), Rotation.NONE, EntityLooks.PLAIN, life));
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(1, uuid, "minecraft:zombie", AT, Rotation.NONE, null, life));
        assertThrows(IllegalArgumentException.class, () -> new EntityLooks(Optional.of(""), false));
        assertThrows(IllegalArgumentException.class, () -> new EntityLooks(Optional.of("x".repeat(EntityLooks.MAX_NAME_LENGTH + 1)), false));
        assertThrows(IllegalArgumentException.class, () -> new EntityLife(-1, 5));
        assertThrows(IllegalArgumentException.class, () -> new EntityLife(Double.NaN, 5));
        assertThrows(IllegalArgumentException.class, () -> new Rotation(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new EntityShape(0, 1.95));
        assertThrows(IllegalArgumentException.class, () -> new EntitySettings(0));
    }
}

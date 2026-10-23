package tlm.dev.basalt.core.entity;

import org.junit.jupiter.api.Test;
import tlm.dev.basalt.core.position.Point;

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
    void theDebugEntityIsANamedZombieWithFiveHealth() {
        VirtualEntity zombie = DebugEntityPattern.at(42, AT);

        assertEquals("minecraft:zombie", zombie.type());
        assertEquals("Basalt", zombie.name());
        assertEquals(5, zombie.health());
        assertEquals(AT, zombie.position());
        assertEquals(DebugEntityPattern.at(42, AT).uuid(), zombie.uuid());
        assertNotEquals(DebugEntityPattern.at(43, AT).uuid(), zombie.uuid());
    }

    @Test
    void diesAtTheLastHealthPointAndNeverBelowZero() {
        VirtualEntity zombie = DebugEntityPattern.at(42, AT);

        VirtualEntity wounded = zombie.hurt(4);
        assertFalse(wounded.isDead());
        assertTrue(wounded.hurt(1).isDead());
        assertEquals(0, wounded.hurt(9).health());
        assertEquals(5, zombie.health());
    }

    @Test
    void movingKeepsEverythingElse() {
        VirtualEntity zombie = DebugEntityPattern.at(42, AT);
        Point there = new Point(1033.5, -52, 1030.5);

        VirtualEntity moved = zombie.movedTo(there);

        assertEquals(there, moved.position());
        assertEquals(zombie.id(), moved.id());
        assertEquals(zombie.uuid(), moved.uuid());
    }

    @Test
    void validatesItsFields() {
        UUID uuid = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(0, uuid, "minecraft:zombie", AT, Rotation.NONE, "", 5));
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(1, uuid, "Zombie", AT, Rotation.NONE, "", 5));
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(1, uuid, "minecraft:zombie", new Point(Double.NaN, 0, 0), Rotation.NONE, "", 5));
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(1, uuid, "minecraft:zombie", AT, Rotation.NONE, "x".repeat(65), 5));
        assertThrows(IllegalArgumentException.class, () -> new VirtualEntity(1, uuid, "minecraft:zombie", AT, Rotation.NONE, "", -1));
        assertThrows(IllegalArgumentException.class, () -> new Rotation(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> new EntityShape(0, 1.95));
        assertThrows(IllegalArgumentException.class, () -> new EntitySettings(0));
    }
}

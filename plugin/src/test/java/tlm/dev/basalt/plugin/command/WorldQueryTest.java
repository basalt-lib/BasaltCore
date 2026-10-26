package tlm.dev.basalt.plugin.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldQueryTest {

    private static final UUID DEBUG = UUID.fromString("c4ee853c-af4a-4540-8782-8bc6d4f0e7ff");
    private static final UUID DEMO = UUID.fromString("8f593a85-1e17-4055-a6fa-9729997e9907");
    private static final UUID TWIN = UUID.fromString("8f593a85-9999-4055-a6fa-000000000001");
    private static final List<UUID> OPEN = List.of(DEBUG, DEMO, TWIN);
    private static final Map<String, UUID> NAMED = Map.of("arena", DEBUG);

    @Test
    void picksADebugWorldByItsName() {
        assertEquals(new WorldQuery.Pick.Found(DEBUG), WorldQuery.pick("arena", NAMED, OPEN));
    }

    @Test
    void picksAnyOpenWorldByItsFullIdInAnyCase() {
        assertEquals(new WorldQuery.Pick.Found(DEMO), WorldQuery.pick(DEMO.toString(), NAMED, OPEN));
        assertEquals(new WorldQuery.Pick.Found(DEBUG), WorldQuery.pick(DEBUG.toString().toUpperCase(), NAMED, OPEN));
    }

    @Test
    void picksAWorldByAUniquePrefixOfAtLeastEightCharacters() {
        assertEquals(new WorldQuery.Pick.Found(DEBUG), WorldQuery.pick("c4ee853c", NAMED, OPEN));
        assertEquals(new WorldQuery.Pick.Found(DEMO), WorldQuery.pick("8f593a85-1e", NAMED, OPEN));
    }

    @Test
    void refusesAPrefixSharedBySeveralWorlds() {
        assertEquals(new WorldQuery.Pick.Ambiguous(2), WorldQuery.pick("8f593a85", NAMED, OPEN));
    }

    @Test
    void refusesAnIdOrAPrefixOfNoOpenWorld() {
        assertEquals(new WorldQuery.Pick.UnknownId(), WorldQuery.pick("00000000", NAMED, OPEN));
        assertEquals(new WorldQuery.Pick.UnknownId(), WorldQuery.pick("00000000-0000-0000-0000-000000000000", NAMED, OPEN));
    }

    @Test
    void treatsAShortPrefixOrAnUnknownNameAsAName() {
        assertEquals(new WorldQuery.Pick.UnknownName(), WorldQuery.pick("c4ee853", NAMED, OPEN));
        assertEquals(new WorldQuery.Pick.UnknownName(), WorldQuery.pick("dungeon", NAMED, OPEN));
    }

    @Test
    void forgetsANameWhoseWorldIsNoLongerOpen() {
        assertEquals(new WorldQuery.Pick.UnknownName(), WorldQuery.pick("arena", NAMED, List.of(DEMO)));
    }

    @Test
    void keepsEveryOwnerWithoutAFilterAndOnlyTheNamedOneWithIt() {
        assertTrue(WorldQuery.ownedBy("BasaltDemo", Optional.empty()));
        assertTrue(WorldQuery.ownedBy("BasaltDemo", Optional.of("basaltdemo")));
        assertFalse(WorldQuery.ownedBy("BasaltCore", Optional.of("BasaltDemo")));
        assertFalse(WorldQuery.ownedBy("BasaltDemo", Optional.of("Nobody")));
    }
}

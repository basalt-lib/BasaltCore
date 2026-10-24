package tlm.dev.basalt.core.visibility;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerSeparationTest {

    private static final UUID FIRST_WORLD = new UUID(0, 1);
    private static final UUID SECOND_WORLD = new UUID(0, 2);
    private static final UUID A = new UUID(0, 0xA);
    private static final UUID B = new UUID(0, 0xB);
    private static final UUID C = new UUID(0, 0xC);
    private static final UUID D = new UUID(0, 0xD);

    @Test
    void separatesEveryWorldByDefault() {
        PlayerGroup first = PlayerGroup.of(FIRST_WORLD, false);
        PlayerGroup second = PlayerGroup.of(SECOND_WORLD, false);
        PlayerGroup outside = PlayerGroup.outside();

        assertEquals("""
                A sees B
                B sees A
                """, matrix(first, first, second, outside));
    }

    @Test
    void opensOneWorldToTheOutsideOnly() {
        PlayerGroup first = PlayerGroup.of(FIRST_WORLD, true);
        PlayerGroup second = PlayerGroup.of(SECOND_WORLD, false);

        assertEquals("""
                A sees B
                A sees D
                B sees A
                B sees D
                D sees A
                D sees B
                """, matrix(first, first, second, PlayerGroup.outside()));
    }

    @Test
    void letsEveryoneSeeEveryoneWhenEveryWorldIsOpen() {
        PlayerGroup first = PlayerGroup.of(FIRST_WORLD, true);
        PlayerGroup second = PlayerGroup.of(SECOND_WORLD, true);

        assertEquals(12, matrix(first, first, second, PlayerGroup.outside()).lines().count());
    }

    @Test
    void keepsTheRealWorldAsItIsWithoutAnyVirtualWorld() {
        assertTrue(PlayerGroup.outside().sees(PlayerGroup.outside()));
    }

    @Test
    void keepsTheViewersOfOneClosedWorldTogether() {
        PlayerGroup closed = PlayerGroup.of(FIRST_WORLD, false);

        assertTrue(closed.sees(PlayerGroup.of(FIRST_WORLD, false)));
        assertFalse(closed.sees(PlayerGroup.outside()));
        assertFalse(PlayerGroup.outside().sees(closed));
    }

    @Test
    void refusesAClosedOutside() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerGroup(Optional.empty(), false));
    }

    @Test
    void plansOnlyTheChangesAgainstWhatIsHidden() {
        Map<UUID, PlayerGroup> others = new LinkedHashMap<>();
        others.put(A, PlayerGroup.of(FIRST_WORLD, false));
        others.put(B, PlayerGroup.of(FIRST_WORLD, false));
        others.put(C, PlayerGroup.of(SECOND_WORLD, false));
        others.put(D, PlayerGroup.outside());

        SeparationPlan plan = PlayerSeparation.plan(A, PlayerGroup.of(FIRST_WORLD, false), others, Set.of(B, C));

        assertEquals(List.of(D), plan.hide());
        assertEquals(List.of(B), plan.show());
    }

    @Test
    void plansNothingForThePlayerItself() {
        SeparationPlan plan = PlayerSeparation.plan(A, PlayerGroup.of(FIRST_WORLD, false),
                Map.of(A, PlayerGroup.of(FIRST_WORLD, false)), Set.of());

        assertTrue(plan.isEmpty());
    }

    @Test
    void showsEveryoneBackToAPlayerWhoLeftItsWorld() {
        Map<UUID, PlayerGroup> others = new LinkedHashMap<>();
        others.put(B, PlayerGroup.of(FIRST_WORLD, false));
        others.put(D, PlayerGroup.outside());

        SeparationPlan plan = PlayerSeparation.plan(A, PlayerGroup.outside(), others, Set.of(D));

        assertEquals(List.of(B), plan.hide());
        assertEquals(List.of(D), plan.show());
    }

    private static String matrix(PlayerGroup a, PlayerGroup b, PlayerGroup c, PlayerGroup d) {
        Map<String, PlayerGroup> players = new LinkedHashMap<>();
        players.put("A", a);
        players.put("B", b);
        players.put("C", c);
        players.put("D", d);
        StringBuilder seen = new StringBuilder();
        players.forEach((viewer, viewerGroup) -> players.forEach((other, otherGroup) -> {
            if (!viewer.equals(other) && viewerGroup.sees(otherGroup)) {
                seen.append(viewer).append(" sees ").append(other).append('\n');
            }
        }));
        return seen.toString();
    }
}

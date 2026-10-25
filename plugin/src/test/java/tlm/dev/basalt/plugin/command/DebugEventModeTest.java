package tlm.dev.basalt.plugin.command;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DebugEventModeTest {

    @Test
    void eachModeCancelsOnlyItsKinds() {
        assertEquals(Set.of(), cancelledBy(DebugEventMode.RECORD));
        assertEquals(EnumSet.of(DebugEventKind.BREAK), cancelledBy(DebugEventMode.CANCEL_BREAKS));
        assertEquals(EnumSet.of(DebugEventKind.PLACE), cancelledBy(DebugEventMode.CANCEL_PLACES));
        assertEquals(EnumSet.of(DebugEventKind.USE, DebugEventKind.ENTITY_USE), cancelledBy(DebugEventMode.CANCEL_USES));
        assertEquals(EnumSet.of(DebugEventKind.DAMAGE), cancelledBy(DebugEventMode.CANCEL_DAMAGE));
        assertEquals(Set.of(), cancelledBy(DebugEventMode.DOUBLE_DAMAGE));
        assertEquals(Set.of(), cancelledBy(DebugEventMode.CONSUME_PLACES));
    }

    @Test
    void onlyItsModeDoublesTheDamageOrConsumesThePlacedItem() {
        assertEquals(List.of(DebugEventMode.DOUBLE_DAMAGE),
                Arrays.stream(DebugEventMode.values()).filter(mode -> mode.damageFactor() == 2).toList());
        assertEquals(List.of(DebugEventMode.CONSUME_PLACES),
                Arrays.stream(DebugEventMode.values()).filter(DebugEventMode::consumesPlaces).toList());
    }

    @Test
    void namesEveryModeForTheCommand() {
        assertEquals(List.of("record", "cancel-breaks", "cancel-places", "cancel-uses", "cancel-damage", "double-damage", "consume-places"),
                Arrays.stream(DebugEventMode.values()).map(DebugEventMode::label).toList());
    }

    private static Set<DebugEventKind> cancelledBy(DebugEventMode mode) {
        return Arrays.stream(DebugEventKind.values()).filter(mode::cancels).collect(Collectors.toSet());
    }
}

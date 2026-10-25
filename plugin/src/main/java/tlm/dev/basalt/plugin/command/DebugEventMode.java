package tlm.dev.basalt.plugin.command;

import java.util.EnumSet;
import java.util.Set;

public enum DebugEventMode {
    RECORD("record", EnumSet.noneOf(DebugEventKind.class)),
    CANCEL_BREAKS("cancel-breaks", EnumSet.of(DebugEventKind.BREAK)),
    CANCEL_PLACES("cancel-places", EnumSet.of(DebugEventKind.PLACE)),
    CANCEL_USES("cancel-uses", EnumSet.of(DebugEventKind.USE, DebugEventKind.ENTITY_USE)),
    CANCEL_DAMAGE("cancel-damage", EnumSet.of(DebugEventKind.DAMAGE)),
    DOUBLE_DAMAGE("double-damage", EnumSet.noneOf(DebugEventKind.class)),
    CONSUME_PLACES("consume-places", EnumSet.noneOf(DebugEventKind.class));

    private final String label;
    private final Set<DebugEventKind> cancelled;

    DebugEventMode(String label, Set<DebugEventKind> cancelled) {
        this.label = label;
        this.cancelled = cancelled;
    }

    public String label() {
        return label;
    }

    public boolean cancels(DebugEventKind kind) {
        return cancelled.contains(kind);
    }

    public double damageFactor() {
        return this == DOUBLE_DAMAGE ? 2 : 1;
    }

    public boolean consumesPlaces() {
        return this == CONSUME_PLACES;
    }
}

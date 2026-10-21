package tlm.dev.basalt.core.debug;

import java.util.Locale;

public enum DebugPattern {
    FLAT,
    RAISED;

    public String label() {
        return name().toLowerCase(Locale.ROOT);
    }
}

package tlm.dev.basalt.core.platform;

import java.util.Objects;

public record GameVersion(String name, int protocol) {

    public GameVersion {
        Objects.requireNonNull(name, "name");
        if (name.isBlank()) {
            throw new IllegalArgumentException("game version name is blank");
        }
        if (protocol <= 0) {
            throw new IllegalArgumentException("game protocol must be positive: " + protocol);
        }
    }
}

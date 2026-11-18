package tlm.dev.basalt.core.platform;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum SupportedVersion {
    V1_21_11("1.21.11", 774),
    V26_2("26.2", 776);

    private final String gameName;
    private final int protocol;

    SupportedVersion(String gameName, int protocol) {
        this.gameName = gameName;
        this.protocol = protocol;
    }

    public String gameName() {
        return gameName;
    }

    public static Optional<SupportedVersion> of(GameVersion game) {
        return Arrays.stream(values())
                .filter(version -> version.gameName.equals(game.name()) && version.protocol == game.protocol())
                .findFirst();
    }

    public static String names() {
        return Arrays.stream(values()).map(SupportedVersion::gameName).collect(Collectors.joining(", "));
    }
}

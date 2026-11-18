package tlm.dev.basalt.core.platform;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SupportedVersionTest {

    @Test
    void picksTheModuleOfAKnownVersion() {
        assertEquals(Optional.of(SupportedVersion.V1_21_11), SupportedVersion.of(new GameVersion("1.21.11", 774)));
    }

    @Test
    void refusesAnUnknownNameOrProtocol() {
        assertEquals(Optional.empty(), SupportedVersion.of(new GameVersion("1.21.10", 773)));
        assertEquals(Optional.empty(), SupportedVersion.of(new GameVersion("1.21.11", 775)));
        assertEquals(Optional.empty(), SupportedVersion.of(new GameVersion("26.1", 774)));
    }
}

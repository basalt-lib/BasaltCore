package tlm.dev.basalt.core.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameVersionTest {

    @Test
    void keepsNameAndProtocol() {
        GameVersion version = new GameVersion("1.21.11", 774);

        assertEquals("1.21.11", version.name());
        assertEquals(774, version.protocol());
    }

    @Test
    void rejectsMissingName() {
        assertThrows(NullPointerException.class, () -> new GameVersion(null, 774));
    }

    @Test
    void rejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new GameVersion(" ", 774));
    }

    @Test
    void rejectsZeroProtocol() {
        assertThrows(IllegalArgumentException.class, () -> new GameVersion("1.21.11", 0));
    }

    @Test
    void rejectsNegativeProtocol() {
        assertThrows(IllegalArgumentException.class, () -> new GameVersion("1.21.11", -1));
    }
}

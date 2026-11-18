package tlm.dev.basalt.plugin.facade;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenFacadesTest {

    @Test
    void forgetsTheClosedOnesAtEachCreationSoTheTableStaysAsLargeAsTheOpenOnes() {
        OpenFacades<String> facades = new OpenFacades<>();
        UUID kept = new UUID(0, 0);
        facades.add(kept, "kept", List.of());
        for (int created = 1; created <= 1000; created++) {
            facades.add(new UUID(0, created), "closed later", List.of(kept, new UUID(0, created - 1)));
        }

        assertEquals(3, facades.size());
        assertEquals(Optional.of("kept"), facades.find(kept));
        assertEquals(Optional.empty(), facades.find(new UUID(0, 500)));
    }

    @Test
    void listsOnlyTheOpenOnesInCreationOrder() {
        OpenFacades<String> facades = new OpenFacades<>();
        UUID first = new UUID(1, 1);
        UUID second = new UUID(1, 2);
        UUID third = new UUID(1, 3);
        facades.add(first, "first", List.of());
        facades.add(second, "second", List.of(first));
        facades.add(third, "third", List.of(first, second));

        assertEquals(List.of("first", "third"), facades.all(List.of(third, first)));
        assertEquals(2, facades.size());
    }
}

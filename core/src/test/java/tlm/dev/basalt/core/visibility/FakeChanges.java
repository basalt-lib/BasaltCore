package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.List;
import java.util.UUID;

final class FakeChanges implements ViewChanges {

    private final List<String> events;

    FakeChanges(List<String> events) {
        this.events = events;
    }

    @Override
    public void entered(UUID player, ChunkKey chunk) {
        events.add("entered " + chunk.x() + " " + chunk.z());
    }

    @Override
    public void left(UUID player, ExitCause cause) {
        events.add("left " + cause);
    }
}

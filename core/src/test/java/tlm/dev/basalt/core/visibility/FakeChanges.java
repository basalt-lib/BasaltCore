package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.List;
import java.util.UUID;

final class FakeChanges implements ViewChanges {

    private final List<String> events;

    FakeChanges(List<String> events) {
        this.events = events;
    }

    @Override
    public void entered(UUID player, UUID world, ChunkRange area) {
        events.add("entered " + area.minX() + " " + area.minZ());
    }

    @Override
    public void left(UUID player, UUID world, ExitCause cause) {
        events.add("left " + cause);
    }

    @Override
    public void sent(UUID player, UUID world, ChunkKey chunk) {
        events.add("sent " + chunk.x() + " " + chunk.z());
    }

    @Override
    public void exited(UUID player, UUID world, ExitCause cause) {
        events.add("exited " + cause);
    }
}

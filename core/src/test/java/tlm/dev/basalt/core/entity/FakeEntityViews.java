package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class FakeEntityViews implements EntityViews {

    final Map<UUID, ChunkKey> chunks = new HashMap<>();
    final Map<UUID, Set<Integer>> published = new HashMap<>();

    @Override
    public Optional<ChunkKey> virtualChunk(UUID player) {
        return Optional.ofNullable(chunks.get(player));
    }

    @Override
    public void publish(UUID player, Set<Integer> ids) {
        published.put(player, Set.copyOf(ids));
    }
}

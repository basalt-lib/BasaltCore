package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

final class FakeEntityViews implements EntityViews {

    final Map<UUID, UUID> viewing = new HashMap<>();
    final Map<UUID, ChunkRange> areas = new HashMap<>();
    final Map<UUID, Set<Integer>> published = new HashMap<>();
    final Map<UUID, Set<ChunkKey>> missing = new HashMap<>();

    void lacks(UUID viewer, ChunkKey chunk) {
        missing.computeIfAbsent(viewer, id -> new HashSet<>()).add(chunk);
    }

    @Override
    public Optional<UUID> worldOf(UUID player) {
        return Optional.ofNullable(viewing.get(player));
    }

    @Override
    public Optional<ChunkRange> area(UUID world) {
        return Optional.ofNullable(areas.get(world));
    }

    @Override
    public Set<UUID> viewers(UUID world) {
        Set<UUID> viewers = new TreeSet<>();
        viewing.forEach((player, viewed) -> {
            if (viewed.equals(world)) {
                viewers.add(player);
            }
        });
        return viewers;
    }

    @Override
    public boolean holdsChunk(UUID viewer, ChunkKey chunk) {
        return !missing.getOrDefault(viewer, Set.of()).contains(chunk);
    }

    @Override
    public void publish(UUID world, Set<Integer> ids) {
        published.put(world, Set.copyOf(ids));
    }
}

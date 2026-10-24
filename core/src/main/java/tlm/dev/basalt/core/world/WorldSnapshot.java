package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record WorldSnapshot<C>(WorldLabel label, HostArea host, Map<ChunkKey, C> chunks, Set<UUID> viewers) {

    public WorldSnapshot {
        if (label == null || host == null) {
            throw new IllegalArgumentException("a world snapshot needs its label and its host area");
        }
        chunks = Map.copyOf(chunks);
        viewers = Set.copyOf(viewers);
        for (ChunkKey chunk : chunks.keySet()) {
            if (!host.chunks().contains(chunk)) {
                throw new IllegalArgumentException("chunk " + chunk + " is outside the area " + host.chunks());
            }
        }
    }

    public static <C> WorldSnapshot<C> empty(WorldLabel label, HostArea host) {
        return new WorldSnapshot<>(label, host, Map.of(), Set.of());
    }

    public UUID id() {
        return label.id();
    }

    public Optional<C> chunk(ChunkKey chunk) {
        return Optional.ofNullable(chunks.get(chunk));
    }

    WorldSnapshot<C> withChunks(Map<ChunkKey, C> changed) {
        return new WorldSnapshot<>(label, host, changed, viewers);
    }

    WorldSnapshot<C> withLabel(WorldLabel changed) {
        return new WorldSnapshot<>(changed, host, chunks, viewers);
    }

    WorldSnapshot<C> withViewer(UUID player) {
        Set<UUID> more = new HashSet<>(viewers);
        more.add(player);
        return new WorldSnapshot<>(label, host, chunks, more);
    }

    WorldSnapshot<C> withoutViewer(UUID player) {
        Set<UUID> fewer = new HashSet<>(viewers);
        fewer.remove(player);
        return new WorldSnapshot<>(label, host, chunks, fewer);
    }
}

package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EntityViews {

    Optional<ChunkKey> virtualChunk(UUID player);

    void publish(UUID player, Set<Integer> ids);
}

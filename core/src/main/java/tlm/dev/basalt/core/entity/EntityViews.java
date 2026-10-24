package tlm.dev.basalt.core.entity;

import tlm.dev.basalt.core.position.ChunkRange;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface EntityViews {

    Optional<UUID> worldOf(UUID player);

    Optional<ChunkRange> area(UUID world);

    Set<UUID> viewers(UUID world);

    void publish(UUID world, Set<Integer> ids);
}

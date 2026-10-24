package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ChunkViews {

    void track(UUID player);

    ShowResult show(UUID player, UUID world, ChunkRange area);

    HideResult hide(UUID player, ExitCause cause);

    int hideAll(ExitCause cause);

    Optional<UUID> world(UUID player);

    Optional<ChunkRange> area(UUID player);

    boolean publishEntities(UUID player, Set<Integer> ids);

    void request(UUID player, ChunkKey chunk);

    int sendRequested(int budget);

    void release(UUID world);
}

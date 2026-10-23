package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface WorldViewers {

    boolean enter(UUID player, UUID world, ChunkRange area);

    void exit(UUID player);

    void blockChanged(Set<UUID> viewers, BlockPosition position, int state);

    void chunksChanged(Set<UUID> viewers, List<ChunkKey> chunks);
}

package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;
import java.util.UUID;

public interface RealChunks {

    Optional<RealBlock> firstBlock(UUID world, ChunkKey chunk);
}

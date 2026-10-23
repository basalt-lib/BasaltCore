package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

public interface WorldChunks<C> {

    C empty(ChunkKey chunk, HostArea host);

    C fill(C chunk, BlockBox box, int state);

    int stateAt(C chunk, BlockPosition position);

    int air();
}

package tlm.dev.basalt.core.world;

import tlm.dev.basalt.core.edit.SectionWrite;
import tlm.dev.basalt.core.position.BlockBox;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.List;

public interface WorldChunks<C> {

    C empty(ChunkKey chunk, HostArea host);

    C fill(C chunk, BlockBox box, int state);

    C written(C chunk, List<SectionWrite> sections);

    int stateAt(C chunk, BlockPosition position);

    int air();
}

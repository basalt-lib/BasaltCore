package tlm.dev.basalt.core.overlay;

import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.List;
import java.util.UUID;

public interface OverlayDisplay<L> {

    boolean holdsChunk(UUID player, ChunkKey chunk);

    void show(UUID player, List<OverlayBlock> blocks);

    void showReal(UUID player, UUID world, List<BlockPosition> positions);

    void blockAction(UUID player, OverlayBlock block, int action, int parameter);

    void spawn(UUID player, OverlayEntity<L> entity);

    void move(UUID player, OverlayEntity<L> entity);

    void update(UUID player, OverlayEntity<L> entity);

    void remove(UUID player, List<Integer> entities);
}

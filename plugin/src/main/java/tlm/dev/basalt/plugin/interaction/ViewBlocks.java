package tlm.dev.basalt.plugin.interaction;

import tlm.dev.basalt.core.interaction.PlayerBlocks;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.visibility.ChunkViews;

import java.util.OptionalInt;
import java.util.UUID;

public final class ViewBlocks implements PlayerBlocks {

    private final ChunkViews views;

    public ViewBlocks(ChunkViews views) {
        this.views = views;
    }

    @Override
    public OptionalInt stateAt(UUID player, BlockPosition position) {
        return views.blockAt(player, position);
    }

    @Override
    public boolean set(UUID player, BlockPosition position, int state) {
        return views.setBlock(player, position, state);
    }
}

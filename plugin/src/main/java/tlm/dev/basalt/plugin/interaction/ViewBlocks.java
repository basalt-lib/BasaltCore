package tlm.dev.basalt.plugin.interaction;

import tlm.dev.basalt.core.interaction.PlayerBlocks;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.world.EditResult;
import tlm.dev.basalt.core.world.VirtualWorlds;

import java.util.OptionalInt;
import java.util.UUID;

public final class ViewBlocks implements PlayerBlocks {

    private final VirtualWorlds<?> worlds;

    public ViewBlocks(VirtualWorlds<?> worlds) {
        this.worlds = worlds;
    }

    @Override
    public OptionalInt stateAt(UUID player, BlockPosition position) {
        return worlds.blockSeenBy(player, position);
    }

    @Override
    public boolean set(UUID player, BlockPosition position, int state) {
        EditResult result = worlds.setBlockBy(player, position, state);
        return result == EditResult.CHANGED || result == EditResult.UNCHANGED;
    }
}

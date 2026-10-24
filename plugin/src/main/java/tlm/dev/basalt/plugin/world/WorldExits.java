package tlm.dev.basalt.plugin.world;

import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldListener;

import java.util.UUID;
import java.util.function.Supplier;

public final class WorldExits implements ViewChanges, WorldListener {

    private final Supplier<VirtualWorlds<?>> worlds;
    private final Supplier<ChunkViews> views;

    public WorldExits(Supplier<VirtualWorlds<?>> worlds, Supplier<ChunkViews> views) {
        this.worlds = worlds;
        this.views = views;
    }

    @Override
    public void left(UUID player, UUID world, ExitCause cause) {
        worlds.get().viewerLeft(world, player);
    }

    @Override
    public void emptied(UUID world) {
        views.get().release(world);
    }

    @Override
    public void closed(UUID world) {
        views.get().release(world);
    }
}

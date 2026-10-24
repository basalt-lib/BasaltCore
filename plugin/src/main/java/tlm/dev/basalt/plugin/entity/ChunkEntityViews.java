package tlm.dev.basalt.plugin.entity;

import tlm.dev.basalt.core.entity.EntityViews;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.world.VirtualWorlds;
import tlm.dev.basalt.core.world.WorldSnapshot;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class ChunkEntityViews implements EntityViews {

    private final Supplier<ChunkViews> views;
    private final Supplier<VirtualWorlds<?>> worlds;

    public ChunkEntityViews(Supplier<ChunkViews> views, Supplier<VirtualWorlds<?>> worlds) {
        this.views = views;
        this.worlds = worlds;
    }

    @Override
    public Optional<UUID> worldOf(UUID player) {
        return worlds.get().worldOf(player);
    }

    @Override
    public Optional<ChunkRange> area(UUID world) {
        return worlds.get().snapshot(world).map(snapshot -> snapshot.host().chunks());
    }

    @Override
    public Set<UUID> viewers(UUID world) {
        return worlds.get().snapshot(world).<Set<UUID>>map(WorldSnapshot::viewers).orElse(Set.of());
    }

    @Override
    public void publish(UUID world, Set<Integer> ids) {
        for (UUID viewer : viewers(world)) {
            views.get().publishEntities(viewer, ids);
        }
    }
}

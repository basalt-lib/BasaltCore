package tlm.dev.basalt.plugin.entity;

import tlm.dev.basalt.core.entity.EntityViews;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.visibility.ChunkViews;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class ChunkEntityViews implements EntityViews {

    private final Supplier<ChunkViews> views;

    public ChunkEntityViews(Supplier<ChunkViews> views) {
        this.views = views;
    }

    @Override
    public Optional<ChunkRange> area(UUID player) {
        return views.get().area(player);
    }

    @Override
    public void publish(UUID player, Set<Integer> ids) {
        views.get().publishEntities(player, ids);
    }
}

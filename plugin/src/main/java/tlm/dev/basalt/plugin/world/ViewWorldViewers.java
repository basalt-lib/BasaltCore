package tlm.dev.basalt.plugin.world;

import tlm.dev.basalt.core.interaction.InteractionReplies;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.ShowResult;
import tlm.dev.basalt.core.world.Departure;
import tlm.dev.basalt.core.world.WorldViewers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class ViewWorldViewers implements WorldViewers {

    private final Supplier<ChunkViews> views;
    private final InteractionReplies replies;

    public ViewWorldViewers(Supplier<ChunkViews> views, InteractionReplies replies) {
        this.views = views;
        this.replies = replies;
    }

    @Override
    public boolean enter(UUID player, UUID world, ChunkRange area) {
        return views.get().show(player, world, area) == ShowResult.SHOWN;
    }

    @Override
    public void exit(UUID player, Departure departure) {
        views.get().hide(player, departure == Departure.CLOSED ? ExitCause.WORLD_CLOSED : ExitCause.COMMAND);
    }

    @Override
    public void blockChanged(Set<UUID> viewers, BlockPosition position, int state) {
        for (UUID viewer : viewers) {
            replies.block(viewer, position, state);
        }
    }

    @Override
    public void chunksChanged(Set<UUID> viewers, List<ChunkKey> chunks) {
        for (UUID viewer : viewers) {
            chunks.forEach(chunk -> views.get().request(viewer, chunk));
        }
    }
}

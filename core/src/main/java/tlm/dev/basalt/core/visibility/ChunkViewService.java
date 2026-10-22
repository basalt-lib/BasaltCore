package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.BlockPosition;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;

public final class ChunkViewService<T> implements ChunkViews {

    private final PlayerViews<T> views = new PlayerViews<>();
    private final ViewDisplay<T> display;
    private final ViewChanges changes;

    public ChunkViewService(ViewDisplay<T> display, ViewChanges changes) {
        this.display = display;
        this.changes = changes;
    }

    @Override
    public void track(UUID player) {
        if (views.find(player).isEmpty()) {
            display.attach(player, views.open(player));
        }
    }

    @Override
    public ShowResult show(UUID player, ChunkKey chunk, DebugPattern pattern) {
        Optional<PlayerView<T>> found = views.find(player);
        if (found.isEmpty()) {
            return ShowResult.NOT_TRACKED;
        }
        PlayerView<T> view = found.get();
        if (view.snapshot().isVirtual()) {
            return ShowResult.ALREADY_VIRTUAL;
        }
        T payload = display.buildVirtual(player, chunk, pattern);
        view.enter(chunk, payload);
        if (display.hasChunk(player, chunk)) {
            display.sendVirtual(player, payload);
        }
        changes.entered(player, chunk);
        return ShowResult.SHOWN;
    }

    @Override
    public HideResult hide(UUID player, ExitCause cause) {
        Optional<PlayerView<T>> found = views.find(player);
        if (found.isEmpty()) {
            return HideResult.NOT_VIRTUAL;
        }
        Optional<ChunkKey> left = found.get().exit();
        left.ifPresent(chunk -> display.restartFlyingCheck(player));
        left.filter(chunk -> cause.clientKeepsChunks() && display.hasChunk(player, chunk))
                .ifPresent(chunk -> display.resendReal(player, chunk));
        left.ifPresent(chunk -> changes.left(player, cause));
        if (cause.endsTracking()) {
            views.close(player);
            display.detach(player);
        }
        return left.isPresent() ? HideResult.RESTORED : HideResult.NOT_VIRTUAL;
    }

    @Override
    public OptionalInt blockAt(UUID player, BlockPosition position) {
        Optional<PlayerView<T>> found = views.find(player);
        if (found.isEmpty() || !found.get().snapshot().hides(position.chunk())) {
            return OptionalInt.empty();
        }
        return display.blockAt(found.get().snapshot().virtualPayload(), position);
    }

    @Override
    public boolean setBlock(UUID player, BlockPosition position, int state) {
        Optional<PlayerView<T>> found = views.find(player);
        if (found.isEmpty()) {
            return false;
        }
        ViewSnapshot<T> snapshot = found.get().snapshot();
        if (!snapshot.hides(position.chunk()) || display.blockAt(snapshot.virtualPayload(), position).isEmpty()) {
            return false;
        }
        return found.get().update(display.withBlock(player, snapshot.virtualPayload(), position, state));
    }

    @Override
    public Optional<ChunkKey> virtualChunk(UUID player) {
        return views.find(player).map(PlayerView::snapshot).map(ViewSnapshot::virtualChunk);
    }

    @Override
    public int hideAll(ExitCause cause) {
        int restored = 0;
        for (UUID player : views.players()) {
            if (hide(player, cause) == HideResult.RESTORED) {
                restored++;
            }
        }
        return restored;
    }
}

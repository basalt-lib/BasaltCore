package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.debug.DebugPattern;
import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;
import java.util.UUID;

public final class ChunkViewService<T> implements ChunkViews {

    private final PlayerViews<T> views = new PlayerViews<>();
    private final ViewDisplay<T> display;

    public ChunkViewService(ViewDisplay<T> display) {
        this.display = display;
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
        return ShowResult.SHOWN;
    }

    @Override
    public HideResult hide(UUID player, ExitCause cause) {
        Optional<PlayerView<T>> found = views.find(player);
        if (found.isEmpty()) {
            return HideResult.NOT_VIRTUAL;
        }
        Optional<ChunkKey> left = found.get().exit();
        left.filter(chunk -> cause.clientKeepsChunks() && display.hasChunk(player, chunk))
                .ifPresent(chunk -> display.resendReal(player, chunk));
        if (cause.endsTracking()) {
            views.close(player);
            display.detach(player);
        }
        return left.isPresent() ? HideResult.RESTORED : HideResult.NOT_VIRTUAL;
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

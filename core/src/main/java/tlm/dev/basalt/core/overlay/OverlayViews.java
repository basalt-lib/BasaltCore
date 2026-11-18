package tlm.dev.basalt.core.overlay;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OverlayViews<L> {

    private final Map<UUID, OverlayView<L>> views = new ConcurrentHashMap<>();

    public OverlayView<L> open(UUID player) {
        return views.computeIfAbsent(player, id -> new OverlayView<>());
    }

    public OverlaySnapshot<L> current(UUID player) {
        OverlayView<L> view = views.get(player);
        return view == null ? OverlaySnapshot.none() : view.current();
    }

    void close(UUID player) {
        OverlayView<L> view = views.remove(player);
        if (view != null) {
            view.publish(OverlaySnapshot.none());
        }
    }

    int size() {
        return views.size();
    }
}

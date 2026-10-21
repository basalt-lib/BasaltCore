package tlm.dev.basalt.core.visibility;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlayerViews<T> {

    private final Map<UUID, PlayerView<T>> views = new HashMap<>();

    public PlayerView<T> open(UUID player) {
        return views.computeIfAbsent(player, id -> new PlayerView<>());
    }

    public Optional<PlayerView<T>> find(UUID player) {
        return Optional.ofNullable(views.get(player));
    }

    public void close(UUID player) {
        views.remove(player);
    }

    public Set<UUID> players() {
        return Set.copyOf(views.keySet());
    }
}

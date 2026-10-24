package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkRange;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public final class PlayerView {

    private volatile ViewSnapshot snapshot = ViewSnapshot.real();

    public ViewSnapshot snapshot() {
        return snapshot;
    }

    public boolean enter(UUID world, ChunkRange area) {
        if (snapshot.isVirtual()) {
            return false;
        }
        snapshot = ViewSnapshot.of(world, area);
        return true;
    }

    public boolean publishEntities(Set<Integer> ids) {
        ViewSnapshot current = snapshot;
        if (!current.isVirtual()) {
            return false;
        }
        snapshot = current.withEntities(ids);
        return true;
    }

    public Optional<ViewSnapshot> exit() {
        ViewSnapshot previous = snapshot;
        if (!previous.isVirtual()) {
            return Optional.empty();
        }
        snapshot = ViewSnapshot.real();
        return Optional.of(previous);
    }
}

package tlm.dev.basalt.core.visibility;

import tlm.dev.basalt.core.position.ChunkKey;

import java.util.Optional;

public final class PlayerView<T> {

    private volatile ViewSnapshot<T> snapshot = ViewSnapshot.real();

    public ViewSnapshot<T> snapshot() {
        return snapshot;
    }

    public boolean enter(ChunkKey chunk, T payload) {
        if (snapshot.isVirtual()) {
            return false;
        }
        snapshot = ViewSnapshot.virtual(chunk, payload);
        return true;
    }

    public boolean update(T payload) {
        ViewSnapshot<T> current = snapshot;
        if (!current.isVirtual()) {
            return false;
        }
        snapshot = ViewSnapshot.virtual(current.virtualChunk(), payload);
        return true;
    }

    public Optional<ChunkKey> exit() {
        ViewSnapshot<T> previous = snapshot;
        if (!previous.isVirtual()) {
            return Optional.empty();
        }
        snapshot = ViewSnapshot.real();
        return Optional.of(previous.virtualChunk());
    }
}

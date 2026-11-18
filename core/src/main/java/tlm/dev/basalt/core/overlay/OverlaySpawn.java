package tlm.dev.basalt.core.overlay;

import java.util.Objects;
import java.util.Optional;

public record OverlaySpawn<L>(OverlayEdit result, Optional<OverlayEntity<L>> entity) {

    public OverlaySpawn {
        Objects.requireNonNull(result, "result");
        Objects.requireNonNull(entity, "entity");
        if ((result == OverlayEdit.CHANGED) != entity.isPresent()) {
            throw new IllegalArgumentException("a spawn holds its entity exactly when it changed the overlay");
        }
    }

    static <L> OverlaySpawn<L> refused(OverlayEdit result) {
        return new OverlaySpawn<>(result, Optional.empty());
    }
}

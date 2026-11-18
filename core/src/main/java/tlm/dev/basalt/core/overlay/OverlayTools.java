package tlm.dev.basalt.core.overlay;

import java.util.UUID;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public record OverlayTools<L>(OverlayDisplay<L> display, OverlayViews<L> views, Supplier<UUID> ids, IntSupplier entityIds) {

    public OverlayTools {
        if (display == null || views == null || ids == null || entityIds == null) {
            throw new IllegalArgumentException("overlays need their display, their views, the ids and the entity ids");
        }
    }
}

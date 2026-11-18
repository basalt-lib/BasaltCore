package tlm.dev.basalt.plugin.overlay;

import tlm.dev.basalt.core.overlay.OverlayExit;
import tlm.dev.basalt.core.overlay.Overlays;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

import java.util.logging.Logger;

public final class OverlaySessions implements Lifecycle {

    private final Overlays<?> overlays;
    private final Logger logger;

    public OverlaySessions(Overlays<?> overlays, Logger logger) {
        this.overlays = overlays;
        this.logger = logger;
    }

    @Override
    public void onDisable() {
        int closed = overlays.closeAll(OverlayExit.SHUTDOWN);
        logger.info("Closed " + closed + " overlay" + (closed == 1 ? "" : "s"));
    }
}

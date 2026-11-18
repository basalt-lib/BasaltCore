package tlm.dev.basalt.plugin.overlay;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import tlm.dev.basalt.core.overlay.OverlayExit;
import tlm.dev.basalt.core.overlay.Overlays;

public final class OverlayWorldListener implements Listener {

    private final Overlays<?> overlays;

    public OverlayWorldListener(Overlays<?> overlays) {
        this.overlays = overlays;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        overlays.removeViewer(event.getPlayer().getUniqueId(), OverlayExit.REAL_WORLD_CHANGE);
    }
}

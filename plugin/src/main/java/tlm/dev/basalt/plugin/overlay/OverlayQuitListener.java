package tlm.dev.basalt.plugin.overlay;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import tlm.dev.basalt.core.overlay.Overlays;

public final class OverlayQuitListener implements Listener {

    private final Overlays<?> overlays;

    public OverlayQuitListener(Overlays<?> overlays) {
        this.overlays = overlays;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        overlays.forget(event.getPlayer().getUniqueId());
    }
}

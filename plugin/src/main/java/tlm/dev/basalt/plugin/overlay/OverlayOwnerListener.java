package tlm.dev.basalt.plugin.overlay;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.plugin.Plugin;
import tlm.dev.basalt.core.overlay.OverlayExit;
import tlm.dev.basalt.core.overlay.Overlays;

public final class OverlayOwnerListener implements Listener {

    private final Plugin basalt;
    private final Overlays<?> overlays;

    public OverlayOwnerListener(Plugin basalt, Overlays<?> overlays) {
        this.basalt = basalt;
        this.overlays = overlays;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDisable(PluginDisableEvent event) {
        Plugin owner = event.getPlugin();
        if (owner.equals(basalt)) {
            return;
        }
        int closed = overlays.closeOwnedBy(owner.getName(), OverlayExit.CLOSED);
        if (closed > 0) {
            basalt.getLogger().info("Closed " + closed + " overlay" + (closed == 1 ? "" : "s") + " of " + owner.getName());
        }
    }
}

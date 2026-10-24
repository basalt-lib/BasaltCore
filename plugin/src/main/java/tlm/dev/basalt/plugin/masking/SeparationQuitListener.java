package tlm.dev.basalt.plugin.masking;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public final class SeparationQuitListener implements Listener {

    private final PlayerSeparations separations;

    public SeparationQuitListener(PlayerSeparations separations) {
        this.separations = separations;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        separations.forget(event.getPlayer().getUniqueId());
    }
}

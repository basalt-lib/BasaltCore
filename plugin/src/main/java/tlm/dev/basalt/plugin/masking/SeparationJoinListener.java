package tlm.dev.basalt.plugin.masking;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public final class SeparationJoinListener implements Listener {

    private final PlayerSeparations separations;

    public SeparationJoinListener(PlayerSeparations separations) {
        this.separations = separations;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        separations.joined(event.getPlayer().getUniqueId());
    }
}

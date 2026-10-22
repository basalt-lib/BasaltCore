package tlm.dev.basalt.plugin.masking;

import io.papermc.paper.event.player.PlayerTrackEntityEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class EntityTrackListener implements Listener {

    private final MaskedViewers viewers;

    public EntityTrackListener(MaskedViewers viewers) {
        this.viewers = viewers;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTrack(PlayerTrackEntityEvent event) {
        if (viewers.hidesOnTrack(event.getPlayer(), event.getEntity())) {
            event.setCancelled(true);
        }
    }
}

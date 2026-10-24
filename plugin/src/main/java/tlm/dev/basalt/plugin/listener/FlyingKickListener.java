package tlm.dev.basalt.plugin.listener;

import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.MovementPolicy;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

import java.util.logging.Logger;

public final class FlyingKickListener implements Listener, Lifecycle {

    private final ChunkViews views;
    private final Logger logger;
    private long cancelled;

    public FlyingKickListener(ChunkViews views, Logger logger) {
        this.views = views;
        this.logger = logger;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onKick(PlayerKickEvent event) {
        if (event.getCause() != PlayerKickEvent.Cause.FLYING_PLAYER) {
            return;
        }
        Location at = event.getPlayer().getLocation();
        if (MovementPolicy.relaxes(views.area(event.getPlayer().getUniqueId()), at.getX(), at.getZ())) {
            event.setCancelled(true);
            cancelled++;
        }
    }

    @Override
    public void onDisable() {
        logger.info("Cancelled " + cancelled + " flying kicks of players in a virtual chunk");
    }
}

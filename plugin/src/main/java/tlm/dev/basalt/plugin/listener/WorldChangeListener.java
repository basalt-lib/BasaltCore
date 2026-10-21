package tlm.dev.basalt.plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ExitCause;

public final class WorldChangeListener implements Listener {

    private final ChunkViews views;

    public WorldChangeListener(ChunkViews views) {
        this.views = views;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        views.hide(event.getPlayer().getUniqueId(), ExitCause.WORLD_CHANGE);
    }
}

package tlm.dev.basalt.plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ExitCause;

public final class PlayerQuitListener implements Listener {

    private final ChunkViews views;

    public PlayerQuitListener(ChunkViews views) {
        this.views = views;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        views.hide(event.getPlayer().getUniqueId(), ExitCause.DISCONNECT);
    }
}

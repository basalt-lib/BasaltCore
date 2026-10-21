package tlm.dev.basalt.plugin.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import tlm.dev.basalt.core.visibility.ChunkViews;

public final class PlayerJoinListener implements Listener {

    private final ChunkViews views;

    public PlayerJoinListener(ChunkViews views) {
        this.views = views;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        views.track(event.getPlayer().getUniqueId());
    }
}

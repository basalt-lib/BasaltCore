package tlm.dev.basalt.plugin.listener;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

import java.util.logging.Logger;

public final class ViewSessions implements Lifecycle {

    private final ChunkViews views;
    private final Server server;
    private final Logger logger;

    public ViewSessions(ChunkViews views, Server server, Logger logger) {
        this.views = views;
        this.server = server;
        this.logger = logger;
    }

    @Override
    public void onEnable() {
        for (Player player : server.getOnlinePlayers()) {
            views.track(player.getUniqueId());
        }
    }

    @Override
    public void onDisable() {
        int restored = views.hideAll(ExitCause.SHUTDOWN);
        logger.info("Restored " + restored + " virtual chunk view" + (restored == 1 ? "" : "s"));
    }
}

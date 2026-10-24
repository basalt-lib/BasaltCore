package tlm.dev.basalt.plugin.world;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import tlm.dev.basalt.core.visibility.ChunkViews;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

public final class ViewSender implements Lifecycle {

    private final Plugin plugin;
    private final ChunkViews views;
    private final int sendsPerTick;
    private BukkitTask task;
    private long sent;
    private int mostInOneTick;

    public ViewSender(Plugin plugin, ChunkViews views, int sendsPerTick) {
        this.plugin = plugin;
        this.views = views;
        this.sendsPerTick = sendsPerTick;
    }

    @Override
    public void onEnable() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::send, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        plugin.getLogger().info("Sent " + sent + " virtual chunks to viewers, " + mostInOneTick + " in one tick at most");
    }

    private void send() {
        int now = views.sendRequested(sendsPerTick);
        sent += now;
        mostInOneTick = Math.max(mostInOneTick, now);
    }
}

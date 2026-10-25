package tlm.dev.basalt.plugin.entity;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

public final class EntityTicker implements Lifecycle {

    private final Plugin plugin;
    private final VirtualEntityService entities;
    private BukkitTask task;

    public EntityTicker(Plugin plugin, VirtualEntityService entities) {
        this.plugin = plugin;
        this.entities = entities;
    }

    @Override
    public void onEnable() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, entities::tick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }
}

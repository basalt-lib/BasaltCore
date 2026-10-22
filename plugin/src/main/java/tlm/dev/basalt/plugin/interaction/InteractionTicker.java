package tlm.dev.basalt.plugin.interaction;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import tlm.dev.basalt.core.interaction.InteractionService;
import tlm.dev.basalt.plugin.lifecycle.Lifecycle;

public final class InteractionTicker implements Lifecycle {

    private final Plugin plugin;
    private final InteractionQueues queues;
    private final InteractionService service;
    private BukkitTask task;

    public InteractionTicker(Plugin plugin, InteractionQueues queues, InteractionService service) {
        this.plugin = plugin;
        this.queues = queues;
        this.service = service;
    }

    @Override
    public void onEnable() {
        task = plugin.getServer().getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        queues.drainEach(service::handle);
        service.tickDelayedBreaks();
    }
}

package tlm.dev.basalt.plugin.interaction;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import tlm.dev.basalt.core.interaction.InteractionService;

public final class InteractionSessions implements Listener {

    private final InteractionQueues queues;
    private final InteractionService service;

    public InteractionSessions(InteractionQueues queues, InteractionService service) {
        this.queues = queues;
        this.service = service;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        queues.forget(event.getPlayer().getUniqueId());
        service.forget(event.getPlayer().getUniqueId());
    }
}

package tlm.dev.basalt.plugin.entity;

import io.papermc.paper.event.packet.PlayerChunkLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.visibility.ChunkViews;

import java.util.Optional;
import java.util.UUID;

public final class EntityResender implements Listener {

    private final ChunkViews views;
    private final VirtualEntityService entities;

    public EntityResender(ChunkViews views, VirtualEntityService entities) {
        this.views = views;
        this.entities = entities;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkSent(PlayerChunkLoadEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        if (views.virtualChunk(player).equals(Optional.of(new ChunkKey(event.getChunk().getX(), event.getChunk().getZ())))) {
            entities.resend(player);
        }
    }
}

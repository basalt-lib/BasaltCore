package tlm.dev.basalt.plugin.world;

import org.bukkit.Server;
import org.bukkit.entity.Player;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.api.event.VirtualViewerAddEvent;
import tlm.dev.basalt.api.event.VirtualViewerRemoveEvent;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.ViewChanges;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class ViewerEvents implements ViewChanges {

    private final Supplier<Basalt> basalt;
    private final Server server;

    public ViewerEvents(Supplier<Basalt> basalt, Server server) {
        this.basalt = basalt;
        this.server = server;
    }

    @Override
    public void entered(UUID player, UUID world, ChunkRange area) {
        Player online = server.getPlayer(player);
        if (online != null) {
            world(world).ifPresent(found -> new VirtualViewerAddEvent(found, online).callEvent());
        }
    }

    @Override
    public void exited(UUID player, UUID world, ExitCause cause) {
        Player online = server.getPlayer(player);
        if (online != null) {
            world(world).ifPresent(found -> new VirtualViewerRemoveEvent(found, online, reasonOf(cause)).callEvent());
        }
    }

    static VirtualViewerRemoveEvent.Reason reasonOf(ExitCause cause) {
        return switch (cause) {
            case COMMAND -> VirtualViewerRemoveEvent.Reason.REQUESTED;
            case DISCONNECT -> VirtualViewerRemoveEvent.Reason.DISCONNECT;
            case WORLD_CHANGE -> VirtualViewerRemoveEvent.Reason.REAL_WORLD_CHANGE;
            case WORLD_CLOSED, SHUTDOWN -> VirtualViewerRemoveEvent.Reason.WORLD_CLOSED;
        };
    }

    private Optional<VirtualWorld> world(UUID id) {
        return basalt.get().worlds().stream().filter(open -> open.id().equals(id)).findFirst();
    }
}

package tlm.dev.basalt.plugin.entity;

import org.bukkit.Server;
import tlm.dev.basalt.api.Basalt;
import tlm.dev.basalt.api.VirtualWorld;
import tlm.dev.basalt.api.event.VirtualEntityDeathEvent;
import tlm.dev.basalt.core.entity.EntityEvents;
import tlm.dev.basalt.core.entity.VirtualEntity;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class BukkitEntityEvents implements EntityEvents {

    private final Supplier<Basalt> basalt;
    private final Server server;

    public BukkitEntityEvents(Supplier<Basalt> basalt, Server server) {
        this.basalt = basalt;
        this.server = server;
    }

    @Override
    public void died(UUID world, VirtualEntity entity, Optional<UUID> killer) {
        basalt.get().worlds().stream()
                .filter(open -> open.id().equals(world))
                .map(VirtualWorld::entities)
                .flatMap(entities -> entities.stream().filter(found -> found.id() == entity.id()))
                .findFirst()
                .ifPresent(dying -> new VirtualEntityDeathEvent(dying, killer.map(server::getPlayer).orElse(null)).callEvent());
    }
}

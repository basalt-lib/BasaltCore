package tlm.dev.basalt.plugin.entity;

import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.ViewChanges;

import java.util.UUID;

public final class EntityExits implements ViewChanges {

    private final VirtualEntityService entities;

    public EntityExits(VirtualEntityService entities) {
        this.entities = entities;
    }

    @Override
    public void left(UUID player, UUID world, ExitCause cause) {
        if (cause.clientKeepsChunks()) {
            entities.removeAll(player);
        } else {
            entities.forget(player);
        }
    }

    @Override
    public void sent(UUID player, UUID world, ChunkKey chunk) {
        entities.resend(player);
    }
}

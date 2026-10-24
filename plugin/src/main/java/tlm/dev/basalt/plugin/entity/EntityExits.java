package tlm.dev.basalt.plugin.entity;

import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.position.ChunkKey;
import tlm.dev.basalt.core.position.ChunkRange;
import tlm.dev.basalt.core.visibility.ExitCause;
import tlm.dev.basalt.core.visibility.ViewChanges;
import tlm.dev.basalt.core.world.WorldListener;

import java.util.UUID;

public final class EntityExits implements ViewChanges, WorldListener {

    private final VirtualEntityService entities;

    public EntityExits(VirtualEntityService entities) {
        this.entities = entities;
    }

    @Override
    public void entered(UUID player, UUID world, ChunkRange area) {
        entities.viewerEntered(world);
    }

    @Override
    public void left(UUID player, UUID world, ExitCause cause) {
        entities.viewerLeft(player, world, cause.clientKeepsChunks());
    }

    @Override
    public void sent(UUID player, UUID world, ChunkKey chunk) {
        entities.resend(player, world, chunk);
    }

    @Override
    public void closed(UUID world) {
        entities.forgetWorld(world);
    }
}

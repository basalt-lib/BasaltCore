package tlm.dev.basalt.plugin.interaction;

import tlm.dev.basalt.core.entity.EntityShapes;
import tlm.dev.basalt.core.interaction.EntityHit;
import tlm.dev.basalt.core.interaction.EntityTarget;
import tlm.dev.basalt.core.interaction.PlayerEntities;
import tlm.dev.basalt.core.overlay.Overlays;

import java.util.Optional;
import java.util.UUID;

public final class OverlayEntityTargets implements PlayerEntities {

    private final Overlays<?> overlays;
    private final EntityShapes shapes;

    public OverlayEntityTargets(Overlays<?> overlays, EntityShapes shapes) {
        this.overlays = overlays;
        this.shapes = shapes;
    }

    @Override
    public Optional<EntityTarget> target(UUID player, int entity) {
        return overlays.entitySeenBy(player, entity).flatMap(found -> shapes.shapeOf(found.type())
                .map(shape -> new EntityTarget(found.position(), shape.width(), shape.height())));
    }

    @Override
    public boolean hitReady(UUID player, int entity) {
        return true;
    }

    @Override
    public EntityHit attack(UUID player, int entity, double damage) {
        return EntityHit.GONE;
    }

    @Override
    public boolean use(UUID player, int entity) {
        return overlays.entitySeenBy(player, entity).isPresent();
    }
}

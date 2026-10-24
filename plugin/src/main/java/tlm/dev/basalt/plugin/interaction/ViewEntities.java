package tlm.dev.basalt.plugin.interaction;

import tlm.dev.basalt.core.entity.EntityShapes;
import tlm.dev.basalt.core.entity.VirtualEntity;
import tlm.dev.basalt.core.entity.VirtualEntityService;
import tlm.dev.basalt.core.interaction.EntityHit;
import tlm.dev.basalt.core.interaction.EntityTarget;
import tlm.dev.basalt.core.interaction.PlayerEntities;

import java.util.Optional;
import java.util.UUID;

public final class ViewEntities implements PlayerEntities {

    private final VirtualEntityService entities;
    private final EntityShapes shapes;

    public ViewEntities(VirtualEntityService entities, EntityShapes shapes) {
        this.entities = entities;
        this.shapes = shapes;
    }

    @Override
    public Optional<EntityTarget> target(UUID player, int entity) {
        Optional<VirtualEntity> found = entities.find(player, entity);
        return found.flatMap(virtual -> shapes.shapeOf(virtual.type())
                .map(shape -> new EntityTarget(virtual.position(), shape.width(), shape.height())));
    }

    @Override
    public EntityHit attack(UUID player, int entity, double damage) {
        return switch (entities.attack(player, entity, damage)) {
            case HURT -> EntityHit.HURT;
            case KILLED -> EntityHit.KILLED;
            case TOO_SOON -> EntityHit.TOO_SOON;
            case GONE -> EntityHit.GONE;
        };
    }

    @Override
    public boolean use(UUID player, int entity) {
        return entities.use(player, entity);
    }
}

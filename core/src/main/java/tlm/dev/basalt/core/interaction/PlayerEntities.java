package tlm.dev.basalt.core.interaction;

import java.util.Optional;
import java.util.UUID;

public interface PlayerEntities {

    Optional<EntityTarget> target(UUID player, int entity);

    boolean hitReady(UUID player, int entity);

    EntityHit attack(UUID player, int entity, double damage);

    boolean use(UUID player, int entity);
}

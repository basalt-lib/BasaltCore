package tlm.dev.basalt.core.interaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

final class FakePlayerEntities implements PlayerEntities {

    final Map<Integer, EntityTarget> targets = new HashMap<>();
    final List<String> calls = new ArrayList<>();
    EntityHit nextHit = EntityHit.HURT;

    @Override
    public Optional<EntityTarget> target(UUID player, int entity) {
        return Optional.ofNullable(targets.get(entity));
    }

    @Override
    public EntityHit attack(UUID player, int entity) {
        calls.add("attack #" + entity);
        return nextHit;
    }

    @Override
    public boolean use(UUID player, int entity) {
        calls.add("use #" + entity);
        return targets.containsKey(entity);
    }
}

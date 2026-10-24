package tlm.dev.basalt.core.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

final class FakeEntityEvents implements EntityEvents {

    final List<String> deaths = new ArrayList<>();
    Consumer<VirtualEntity> onDeath = entity -> {
    };

    @Override
    public void died(UUID world, VirtualEntity entity, Optional<UUID> killer) {
        deaths.add("died #" + entity.id() + " by " + killer.map(FakeEntityDisplay::name).orElse("none"));
        onDeath.accept(entity);
    }
}

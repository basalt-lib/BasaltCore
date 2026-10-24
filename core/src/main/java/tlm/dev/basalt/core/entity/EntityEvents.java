package tlm.dev.basalt.core.entity;

import java.util.Optional;
import java.util.UUID;

public interface EntityEvents {

    void died(UUID world, VirtualEntity entity, Optional<UUID> killer);
}

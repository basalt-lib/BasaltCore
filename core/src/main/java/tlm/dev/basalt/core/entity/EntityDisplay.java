package tlm.dev.basalt.core.entity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EntityDisplay {

    void spawn(UUID viewer, VirtualEntity entity);

    void move(UUID viewer, VirtualEntity entity);

    void update(UUID viewer, VirtualEntity entity);

    void hurt(UUID viewer, VirtualEntity entity, UUID attacker);

    void die(UUID viewer, VirtualEntity entity, Optional<UUID> killer);

    void remove(UUID viewer, List<Integer> ids);

    void removeDead(UUID viewer, List<Integer> ids);
}

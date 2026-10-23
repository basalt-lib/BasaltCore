package tlm.dev.basalt.core.entity;

import java.util.List;
import java.util.UUID;

public interface EntityDisplay {

    void spawn(UUID player, VirtualEntity entity);

    void move(UUID player, VirtualEntity entity);

    void hurt(UUID player, VirtualEntity entity);

    void die(UUID player, VirtualEntity entity);

    void remove(UUID player, List<Integer> ids);

    void removeDead(UUID player, List<Integer> ids);
}
